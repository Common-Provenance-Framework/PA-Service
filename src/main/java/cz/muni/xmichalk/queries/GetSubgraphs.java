package cz.muni.xmichalk.queries;

import java.util.List;
import java.util.stream.Collectors;

import org.openprovenance.prov.model.Document;

import cz.muni.xmichalk.models.SubgraphWrapper;
import cz.muni.xmichalk.querySpecification.findable.IFindableSubgraph;
import cz.muni.xmichalk.storage.EBundlePart;
import cz.muni.xmichalk.util.ResultsTransformationUtils;
import tools.jackson.databind.JsonNode;

public class GetSubgraphs extends FindSubgraphsQuery<List<JsonNode>> {
    public GetSubgraphs() {
    }

    public GetSubgraphs(IFindableSubgraph subgraph) {

        this.fromSubgraphs = subgraph;
    }

    @Override
    protected EBundlePart decideRequiredBundlePart() {
        return EBundlePart.Whole;
    }

    @Override
    protected List<JsonNode> transformResult(final List<SubgraphWrapper> subgraphs) {
        if (subgraphs == null || subgraphs.isEmpty()) {
            return List.of();
        }
        return subgraphs.stream().map(this::transformSubgraphToDocJson).collect(Collectors.toList());
    }

    private JsonNode transformSubgraphToDocJson(SubgraphWrapper subgraph) {
        if (subgraph == null || subgraph.getNodes() == null) {
            return null;
        }

        Document encapsulatigDocument = ResultsTransformationUtils.encapsulateInDocument(subgraph.getNodes(),
                subgraph.getEdges());

        return ResultsTransformationUtils.transformToJsonNode(encapsulatigDocument);
    }
}
