package info.isaksson.erland.architecturebrowser.indexer.extract;

import info.isaksson.erland.architecturebrowser.indexer.parse.SyntaxNode;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

public final class AngularDecoratorModelExtractor {
    private static final Set<String> SUPPORTED_DECORATORS = Set.of("Component", "Directive", "Pipe", "NgModule", "Injectable");

    private AngularDecoratorModelExtractor() {
    }

    public static Optional<AngularDecoratorModel> extract(SyntaxNode declarationNode) {
        if (declarationNode == null) {
            return Optional.empty();
        }
        List<SyntaxNode> decorators = SyntaxTreeExtractionSupport.descendantsByType(declarationNode, Set.of("decorator"));
        if (decorators.isEmpty()) {
            return Optional.empty();
        }
        for (SyntaxNode decoratorNode : decorators) {
            String snippet = decoratorNode.textSnippet();
            Optional<String> decoratorName = decoratorName(snippet);
            if (decoratorName.isEmpty() || !SUPPORTED_DECORATORS.contains(decoratorName.get())) {
                continue;
            }
            return Optional.of(new AngularDecoratorModel(
                decoratorName.get(),
                angularKind(decoratorName.get()),
                AngularLiteralSupport.topLevelObjectFields(AngularLiteralSupport.firstObjectLiteral(snippet))
            ));
        }
        return Optional.empty();
    }

    private static Optional<String> decoratorName(String snippet) {
        List<String> annotations = SyntaxTreeExtractionSupport.extractAnnotationsFromSnippet(snippet);
        if (annotations.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(annotations.getFirst());
    }

    private static String angularKind(String decoratorName) {
        return switch (decoratorName) {
            case "Component" -> "component";
            case "Directive" -> "directive";
            case "Pipe" -> "pipe";
            case "NgModule" -> "module";
            case "Injectable" -> "injectable";
            default -> "";
        };
    }
}
