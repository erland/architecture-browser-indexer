package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.extract.model.ExtractedEntityFact;
import info.isaksson.erland.architecturebrowser.indexer.ir.model.SourceReference;
import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

final class JavaGenericSyntaxSupport {
    private JavaGenericSyntaxSupport() {}

    static List<String> extractExtendedTypes(SyntaxNode typeNode) {
        return JavaRelationshipEvidenceEmitter.extractExtendedTypes(typeNode);
    }

    static boolean isConstructor(ExtractedEntityFact methodEntity) {
        if (methodEntity == null) {
            return false;
        }
        Object ownerQualifiedName = methodEntity.metadata().get("ownerQualifiedName");
        if (ownerQualifiedName == null || String.valueOf(ownerQualifiedName).isBlank()) {
            return false;
        }
        String ownerSimpleName = simpleName(String.valueOf(ownerQualifiedName));
        return ownerSimpleName != null && ownerSimpleName.equals(methodEntity.name());
    }

    static String simpleName(String qualifiedName) {
        if (qualifiedName == null || qualifiedName.isBlank()) {
            return null;
        }
        int idx = qualifiedName.lastIndexOf('.');
        return idx >= 0 ? qualifiedName.substring(idx + 1) : qualifiedName;
    }

    static Optional<String> inferJavaMethodReturnTypeFromSnippet(String snippet, String methodName) {
        if (snippet == null || snippet.isBlank() || methodName == null || methodName.isBlank()) {
            return Optional.empty();
        }
        Pattern pattern = Pattern.compile("(?:public|protected|private)?\\s*(?:static\\s+)?(?:final\\s+)?([A-Za-z_$][\\w$<>., ?]+?)\\s+" + Pattern.quote(methodName) + "\\s*\\(");
        Matcher matcher = pattern.matcher(snippet.replace("\n", " "));
        if (matcher.find()) {
            return Optional.ofNullable(matcher.group(1)).map(String::trim).filter(value -> !value.isBlank());
        }
        return Optional.empty();
    }

    static boolean isJavaTypeDeclaration(SyntaxNode node) {
        return node != null && Set.of(
            "class_declaration", "interface_declaration", "enum_declaration", "record_declaration"
        ).contains(node.type());
    }

    static int lineOf(SourceReference ref, SyntaxNode fallbackNode) {
        return JavaSourceReferenceSupport.lineOf(ref, fallbackNode);
    }

    static String exactNodeSnippet(String sourceText, SyntaxNode node) {
        return JavaSourceReferenceSupport.exactNodeSnippet(sourceText, node);
    }

    static Optional<String> importQualifiedName(String snippet) {
        return SyntaxTreeExtractionSupport.extractQualifiedName(
            snippet == null ? null : snippet.replaceFirst("^\\s*import\\s+", "").replaceFirst(";\\s*$", "")
        );
    }

    static String derivePackageFromPath(String relativePath) {
        int marker = relativePath.indexOf("/java/");
        if (marker >= 0) {
            String candidate = relativePath.substring(marker + 6);
            int slash = candidate.lastIndexOf('/');
            if (slash > 0) {
                return candidate.substring(0, slash).replace('/', '.');
            }
        }
        int slash = relativePath.lastIndexOf('/');
        return slash > 0 ? relativePath.substring(0, slash).replace('/', '.') : "default";
    }
}
