package info.isaksson.erland.architecturebrowser.indexer.extract;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;

class JavaTypedSemanticsModelsTest {
    @Test
    void shapesEndpointObserverAndWritePathMetadataThroughTypedModels() {
        JavaEndpointSemantics endpoint = new JavaEndpointSemantics(
            "POST",
            "/orders",
            "/orders",
            "/",
            "com.example.orders.api.OrderResource",
            "createOrder",
            "com.example.orders.api.OrderResource#createOrder",
            List.of(Map.of("parameterKind", "BODY", "name", "request", "declaredType", "OrderRequest")),
            List.of("POST")
        );
        Map<String, Object> endpointMethodMetadata = endpoint.methodMetadata(Map.of("name", "createOrder"));
        assertEquals(true, endpointMethodMetadata.get("jaxRsEndpoint"));
        assertEquals("POST", endpointMethodMetadata.get("httpMethod"));

        JavaObserverSemantics observer = new JavaObserverSemantics("com.example.OrderCreatedEvent", true, List.of("Critical"));
        Map<String, Object> observerMetadata = observer.methodMetadata(Map.of());
        assertEquals(true, observerMetadata.get("cdiObserver"));
        assertEquals(true, observerMetadata.get("observerAsync"));

        JavaWritePathSemantics writePath = new JavaWritePathSemantics(List.of("persist"), List.of("com.example.OrderEntity"));
        Map<String, Object> writePathMetadata = writePath.methodMetadata(Map.of());
        assertEquals(true, writePathMetadata.get("writePath"));
        assertEquals(List.of("persist"), writePathMetadata.get("writeOperations"));
    }
}
