package com.lutzseverino.minecraftsetup.infrastructure.manifest;

import com.fasterxml.jackson.core.JsonFactory;
import com.fasterxml.jackson.core.StreamReadFeature;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import com.lutzseverino.minecraftsetup.application.ManifestUnavailableException;
import com.lutzseverino.minecraftsetup.domain.ManifestFingerprint;
import com.lutzseverino.minecraftsetup.domain.ManifestSnapshot;
import com.lutzseverino.minecraftsetup.domain.ProfileId;
import com.networknt.schema.InputFormat;
import com.networknt.schema.JsonSchema;
import com.networknt.schema.JsonSchemaFactory;
import com.networknt.schema.SpecVersion;
import com.networknt.schema.ValidationMessage;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.Inet4Address;
import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.UnknownHostException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.text.Normalizer;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import org.erdtman.jcs.JsonCanonicalizer;

public final class ProtocolManifestReader {
    private static final int MAX_MANIFEST_BYTES = 1024 * 1024;
    private static final Pattern WINDOWS_RESERVED = Pattern.compile(
            "(?i)^(CON|PRN|AUX|NUL|COM[1-9]|LPT[1-9])(?:\\..*)?$"
    );
    private static final ObjectMapper JSON = new ObjectMapper(JsonFactory.builder()
            .enable(StreamReadFeature.STRICT_DUPLICATE_DETECTION)
            .build());

    private final Path source;
    private final JsonSchema schema;

    public ProtocolManifestReader(Path source) {
        this.source = source.toAbsolutePath().normalize();
        JsonSchemaFactory factory = JsonSchemaFactory.getInstance(SpecVersion.VersionFlag.V202012);
        java.io.InputStream schemaStream = ProtocolManifestReader.class.getResourceAsStream(
                "/protocol/schema/v1/manifest.schema.json"
        );
        if (schemaStream == null) {
            throw new IllegalStateException("Bundled protocol v1 schema is missing");
        }
        try (schemaStream) {
            this.schema = factory.getSchema(schemaStream);
        } catch (IOException exception) {
            throw new IllegalStateException("Bundled protocol v1 schema is invalid", exception);
        }
    }

    public ManifestSnapshot read() throws ManifestUnavailableException {
        byte[] bytes;
        try {
            long size = Files.size(source);
            if (size > MAX_MANIFEST_BYTES) {
                throw new ManifestUnavailableException("Manifest exceeds the 1 MiB limit");
            }
            bytes = Files.readAllBytes(source);
        } catch (IOException exception) {
            throw new ManifestUnavailableException("Could not read manifest " + source, exception);
        }

        JsonNode root;
        try {
            root = JSON.readTree(bytes);
        } catch (IOException exception) {
            throw new ManifestUnavailableException("Manifest is not valid UTF-8 JSON", exception);
        }
        if (root == null) {
            throw new ManifestUnavailableException("Manifest is empty");
        }

        Set<ValidationMessage> errors = schema.validate(
                root.toString(),
                InputFormat.JSON,
                executionContext -> executionContext.getExecutionConfig().setFormatAssertionsEnabled(true)
        );
        if (!errors.isEmpty()) {
            String detail = errors.stream().map(ValidationMessage::getMessage).sorted().findFirst().orElse("invalid");
            throw new ManifestUnavailableException("Manifest does not match protocol v1: " + detail);
        }

        validateNoNullsAndNfc(root, "$");
        validatePortableFileName(root.path("install").path("gameDirectoryName").asText());
        Set<ProfileId> profiles = readProfiles(root);
        validateResources(root, profiles);
        ManifestFingerprint fingerprint = fingerprint(root);
        return new ManifestSnapshot(fingerprint, profiles, bytes);
    }

    private static Set<ProfileId> readProfiles(JsonNode root) throws ManifestUnavailableException {
        Set<ProfileId> profiles = new HashSet<>();
        for (JsonNode profile : root.path("profiles")) {
            ProfileId id;
            try {
                id = new ProfileId(profile.path("id").asText());
            } catch (IllegalArgumentException exception) {
                throw new ManifestUnavailableException("Manifest contains an unsupported profile ID", exception);
            }
            if (!profiles.add(id)) {
                throw new ManifestUnavailableException("Profile IDs must be unique: " + id.value());
            }
        }
        return Set.copyOf(profiles);
    }

    private static void validateResources(JsonNode root, Set<ProfileId> profiles)
            throws ManifestUnavailableException {
        Set<String> resourceIds = new HashSet<>();
        Set<String> destinations = new HashSet<>();
        for (JsonNode resource : root.path("resources")) {
            String id = resource.path("id").asText();
            if (!resourceIds.add(id)) {
                throw new ManifestUnavailableException("Resource IDs must be unique: " + id);
            }

            String fileName = resource.path("fileName").asText();
            validatePortableFileName(fileName);
            String destination = resource.path("target").asText() + ":"
                    + fileName.toLowerCase(Locale.ROOT);
            if (!destinations.add(destination)) {
                throw new ManifestUnavailableException("Two resources own the same destination: " + fileName);
            }

            Set<ProfileId> references = new HashSet<>();
            for (JsonNode profileReference : resource.path("profiles")) {
                ProfileId profileId = new ProfileId(profileReference.asText());
                if (!profiles.contains(profileId)) {
                    throw new ManifestUnavailableException(
                            "Resource " + id + " refers to unknown profile " + profileId.value()
                    );
                }
                if (!references.add(profileId)) {
                    throw new ManifestUnavailableException(
                            "Resource " + id + " repeats profile " + profileId.value()
                    );
                }
            }

            JsonNode source = resource.path("source");
            if ("direct".equals(source.path("kind").asText())) {
                validatePublicHttpsUrl(source.path("url").asText());
            }
        }
    }

    private static void validatePortableFileName(String fileName) throws ManifestUnavailableException {
        if (fileName.getBytes(StandardCharsets.UTF_8).length > 200) {
            throw new ManifestUnavailableException("Resource filename exceeds 200 UTF-8 bytes");
        }
        if (WINDOWS_RESERVED.matcher(fileName).matches()) {
            throw new ManifestUnavailableException("Resource filename is reserved on Windows: " + fileName);
        }
        if (!fileName.equals(fileName.strip()) || fileName.endsWith(".")) {
            throw new ManifestUnavailableException("Resource filename has unsafe edge characters: " + fileName);
        }
    }

    private static void validatePublicHttpsUrl(String value) throws ManifestUnavailableException {
        try {
            URI uri = new URI(value);
            String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null || uri.getUserInfo() != null) {
                throw new ManifestUnavailableException("Direct resources must use public HTTPS without credentials");
            }
            String normalizedHost = host.toLowerCase(Locale.ROOT);
            if (isExplicitlyNonPublicHost(normalizedHost)) {
                throw new ManifestUnavailableException("Direct resource URL is not public HTTPS");
            }
        } catch (URISyntaxException exception) {
            throw new ManifestUnavailableException("Direct resource URL is invalid", exception);
        }
    }

    private static boolean isExplicitlyNonPublicHost(String host) {
        String normalized = host.replace("[", "").replace("]", "");
        if (normalized.equals("localhost") || normalized.endsWith(".localhost")
                || normalized.endsWith(".local")) {
            return true;
        }
        if (!normalized.contains(":") && !normalized.matches("[0-9.]+")) {
            return false;
        }
        try {
            return isNonPublicAddress(InetAddress.getByName(normalized));
        } catch (UnknownHostException exception) {
            return true;
        }
    }

    private static boolean isNonPublicAddress(InetAddress address) {
        if (address.isAnyLocalAddress() || address.isLoopbackAddress()
                || address.isLinkLocalAddress() || address.isSiteLocalAddress()
                || address.isMulticastAddress()) {
            return true;
        }
        byte[] bytes = address.getAddress();
        if (address instanceof Inet4Address) {
            int first = Byte.toUnsignedInt(bytes[0]);
            int second = Byte.toUnsignedInt(bytes[1]);
            int third = Byte.toUnsignedInt(bytes[2]);
            return first == 0
                    || (first == 100 && second >= 64 && second <= 127)
                    || (first == 192 && second == 0)
                    || (first == 198 && (second == 18 || second == 19))
                    || (first == 198 && second == 51 && third == 100)
                    || (first == 203 && second == 0 && third == 113)
                    || first >= 224;
        }
        return address instanceof Inet6Address && (Byte.toUnsignedInt(bytes[0]) & 0xfe) == 0xfc;
    }

    private static void validateNoNullsAndNfc(JsonNode node, String path)
            throws ManifestUnavailableException {
        if (node.isNull()) {
            throw new ManifestUnavailableException("Explicit null is not allowed at " + path);
        }
        if (node.isTextual() && !Normalizer.isNormalized(node.textValue(), Normalizer.Form.NFC)) {
            throw new ManifestUnavailableException("Text must use Unicode NFC at " + path);
        }
        if (node.isObject()) {
            Iterator<Map.Entry<String, JsonNode>> fields = node.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                if (!Normalizer.isNormalized(field.getKey(), Normalizer.Form.NFC)) {
                    throw new ManifestUnavailableException("Property names must use Unicode NFC at " + path);
                }
                validateNoNullsAndNfc(field.getValue(), path + "." + field.getKey());
            }
        } else if (node.isArray()) {
            for (int index = 0; index < node.size(); index++) {
                validateNoNullsAndNfc(node.get(index), path + "[" + index + "]");
            }
        }
    }

    private static ManifestFingerprint fingerprint(JsonNode source) throws ManifestUnavailableException {
        JsonNode normalized = source.deepCopy();
        for (JsonNode profile : normalized.path("profiles")) {
            ObjectNode object = (ObjectNode) profile;
            object.putIfAbsent("includesShaders", object.booleanNode(false));
        }
        for (JsonNode resource : normalized.path("resources")) {
            ObjectNode object = (ObjectNode) resource;
            object.putIfAbsent("required", object.booleanNode(false));
            object.putIfAbsent("profiles", object.arrayNode());
            object.putIfAbsent("hashes", object.objectNode());
            ArrayNode profileIds = (ArrayNode) object.path("profiles");
            java.util.List<String> sorted = new java.util.ArrayList<>();
            profileIds.forEach(value -> sorted.add(value.asText()));
            sorted.sort(String::compareTo);
            profileIds.removeAll();
            sorted.forEach(profileIds::add);
            ObjectNode hashes = (ObjectNode) object.path("hashes");
            hashes.fieldNames().forEachRemaining(name -> hashes.put(name, hashes.path(name).asText().toLowerCase(Locale.ROOT)));
        }

        try {
            byte[] canonical = new JsonCanonicalizer(normalized.toString()).getEncodedUTF8();
            byte[] digest = MessageDigest.getInstance("SHA-256").digest(canonical);
            return new ManifestFingerprint("msm-v1-sha256:" + java.util.HexFormat.of().formatHex(digest));
        } catch (IOException | NoSuchAlgorithmException exception) {
            throw new ManifestUnavailableException("Could not fingerprint manifest", exception);
        }
    }
}
