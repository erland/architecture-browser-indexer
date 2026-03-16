package info.isaksson.erland.architecturebrowser.indexer.extract;

import java.util.Map;

public record AngularDecoratorModel(
    String decoratorName,
    String angularKind,
    Map<String, String> fields
) {
    public AngularDecoratorModel {
        decoratorName = decoratorName == null ? "" : decoratorName;
        angularKind = angularKind == null ? "" : angularKind;
        fields = fields == null ? Map.of() : Map.copyOf(fields);
    }
}
