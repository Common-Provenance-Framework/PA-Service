package cz.muni.xmichalk.queries;

import cz.muni.fi.cpm.model.CpmDocument;
import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.MockedStorage;
import cz.muni.xmichalk.TestDocumentProvider;
import cz.muni.xmichalk.models.ConnectorData;
import cz.muni.xmichalk.models.QueryContext;
import cz.muni.xmichalk.querySpecification.findable.DerivationPathFromStartNode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.openprovenance.prov.model.QualifiedName;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertThrows;

public class GetConnectorsTest {

    static Stream<Object[]> testParams() {
        return Stream.of(
                new Object[]{
                        TestDocumentProvider.samplingBundle_V0,
                        TestDocumentProvider.samplingBundle_V0.getForwardConnectors().getFirst(),
                        0,
                        2
                }, new Object[]{
                        TestDocumentProvider.samplingBundle_V1,
                        TestDocumentProvider.samplingBundle_V1.getForwardConnectors().getFirst(),
                        0,
                        6
                }, new Object[]{
                        TestDocumentProvider.processingBundle_V0,
                        TestDocumentProvider.processingBundle_V0.getForwardConnectors().getFirst(),
                        1,
                        1
                }, new Object[]{
                        TestDocumentProvider.processingBundle_V1,
                        TestDocumentProvider.processingBundle_V1.getForwardConnectors().getFirst(),
                        1,
                        2
                }
        );
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.MethodSource("testParams")
    public void testGetConnectors_backward_unfiltered(
            CpmDocument cpmDocument,
            INode startNode,
            int backwardConnectors,
            int forwardConnectors
    ) throws AccessDeniedException {
        GetConnectors getAllConnectorsQuery = new GetConnectors(true, (g, n) -> List.of(g));
        QueryContext context =
                new QueryContext(cpmDocument.getBundleId(), startNode.getId(), null, new MockedStorage());

        List<ConnectorData> result = getAllConnectorsQuery.evaluate(context).result;

        assert result.size() == backwardConnectors;
        assert cpmDocument.getBackwardConnectors().stream().allMatch(connectorNode -> result.stream()
                .anyMatch(connectorData -> connectorData.id.toQN().getUri().equals(connectorNode.getId().getUri())));
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.MethodSource("testParams")
    public void testGetConnectors_forward_unfiltered(
            CpmDocument cpmDocument,
            INode startNode,
            int backwardConnectors,
            int forwardConnectors
    ) throws AccessDeniedException {
        GetConnectors getAllConnectorsQuery = new GetConnectors(false, (g, n) -> List.of(g));
        QueryContext context =
                new QueryContext(cpmDocument.getBundleId(), startNode.getId(), null, new MockedStorage());

        List<ConnectorData> result = getAllConnectorsQuery.evaluate(context).result;

        assert result.size() == forwardConnectors;
        assert cpmDocument.getForwardConnectors().stream().allMatch(connectorNode -> result.stream()
                .anyMatch(connectorData -> connectorData.id.toQN().getUri().equals(connectorNode.getId().getUri())));
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.MethodSource("testParams")
    public void testGetConnectors_all_unfiltered(
            CpmDocument cpmDocument,
            INode startNode,
            int backwardConnectors,
            int forwardConnectors
    ) throws AccessDeniedException {
        GetConnectors getAllConnectorsQuery = new GetConnectors(null, (g, n) -> List.of(g));
        QueryContext context =
                new QueryContext(cpmDocument.getBundleId(), startNode.getId(), null, new MockedStorage());

        List<ConnectorData> result = getAllConnectorsQuery.evaluate(context).result;

        assert result.size() == backwardConnectors + forwardConnectors;
    }

    @ParameterizedTest @org.junit.jupiter.params.provider.MethodSource("testParams")
    public void testGetConnectors_all_filteredToNothing(
            CpmDocument cpmDocument,
            INode startNode,
            int backwardConnectors,
            int forwardConnectors
    ) throws AccessDeniedException {
        GetConnectors getAllConnectorsQuery = new GetConnectors(null, (g, n) -> List.of());
        QueryContext context =
                new QueryContext(cpmDocument.getBundleId(), startNode.getId(), null, new MockedStorage());

        List<ConnectorData> result = getAllConnectorsQuery.evaluate(context).result;

        assert result.isEmpty();
    }

    private static final CpmDocument processingBundle = TestDocumentProvider.processingBundle_V0;

    @Test
    public void testGetConnectors_backwardHopEnteringAtBackwardConnector_fails() {
        GetConnectors query = hopQuery(true);
        QueryContext context = contextAt(processingBundle.getBackwardConnectors().getFirst().getId());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> query.evaluate(context));

        assert e.getMessage().contains("is not a forwardConnector");
    }

    @Test
    public void testGetConnectors_backwardHopEnteringAtForwardConnector_findsBackwardConnectors()
            throws AccessDeniedException {
        GetConnectors query = hopQuery(true);
        QueryContext context = contextAt(processingBundle.getForwardConnectors().getFirst().getId());

        List<ConnectorData> result = query.evaluate(context).result;

        assert result.size() == 1;
        assert result.getFirst().id.toQN().equals(processingBundle.getBackwardConnectors().getFirst().getId());
    }

    @Test
    public void testGetConnectors_backwardHopEnteringAtNonConnector_fails() {
        GetConnectors query = hopQuery(true);
        QueryContext context = contextAt(processingBundle.getMainActivity().getId());

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> query.evaluate(context));

        assert e.getMessage().contains("is not a forwardConnector");
    }

    @Test
    public void testGetConnectors_traversalStartAtBackwardConnector_notChecked() throws AccessDeniedException {
        GetConnectors query = new GetConnectors(true, new DerivationPathFromStartNode(true));
        QueryContext context = contextAt(processingBundle.getBackwardConnectors().getFirst().getId());

        List<ConnectorData> result = query.evaluate(context).result;

        assert result.size() == 1;
        assert result.getFirst().id.toQN().equals(processingBundle.getBackwardConnectors().getFirst().getId());
    }

    @Test
    public void testGetConnectors_backwardHopEnteringAtMissingNode_fails() {
        GetConnectors query = hopQuery(true);
        QueryContext context = contextAt(new org.openprovenance.prov.vanilla.QualifiedName(
                "https://openprovenance.org/blank/", "NoSuchCon", "blank"));

        IllegalArgumentException e = assertThrows(IllegalArgumentException.class, () -> query.evaluate(context));

        assert e.getMessage().contains("does not exist");
    }

    @Test
    public void testGetConnectors_forwardHop_notChecked() throws AccessDeniedException {
        GetConnectors query = hopQuery(false);
        QueryContext context = contextAt(processingBundle.getBackwardConnectors().getFirst().getId());

        List<ConnectorData> result = query.evaluate(context).result;

        assert result.size() == 1;
        assert result.getFirst().id.toQN().equals(processingBundle.getForwardConnectors().getFirst().getId());
    }

    private static GetConnectors hopQuery(boolean backward) {
        GetConnectors query = new GetConnectors(backward, new DerivationPathFromStartNode(backward));
        query.enteredViaConnector = true;
        return query;
    }

    private static QueryContext contextAt(QualifiedName startNodeId) {
        return new QueryContext(processingBundle.getBundleId(), startNodeId, null, new MockedStorage());
    }
}
