package cz.muni.xmichalk.queries;

import static cz.muni.xmichalk.util.AttributeNames.ATTR_HASH_ALG;
import static cz.muni.xmichalk.util.AttributeNames.ATTR_PROVENANCE_SERVICE_URI;
import static cz.muni.xmichalk.util.AttributeNames.ATTR_PROV_TYPE;
import static cz.muni.xmichalk.util.AttributeNames.ATTR_REFERENCED_BUNDLE_HASH_VALUE;
import static cz.muni.xmichalk.util.AttributeNames.ATTR_REFERENCED_BUNDLE_ID;
import static cz.muni.xmichalk.util.AttributeNames.ATTR_REFERENCED_META_BUNDLE_ID;
import static cz.muni.xmichalk.util.NameSpaceConstants.CPM_URI;

import java.nio.file.AccessDeniedException;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.openprovenance.prov.model.QualifiedName;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.models.ConnectorData;
import cz.muni.xmichalk.models.QualifiedNameData;
import cz.muni.xmichalk.models.QueryContext;
import cz.muni.xmichalk.models.QueryResult;
import cz.muni.xmichalk.models.SubgraphWrapper;
import cz.muni.xmichalk.querySpecification.findable.FittingNodes;
import cz.muni.xmichalk.querySpecification.findable.IFindableSubgraph;
import cz.muni.xmichalk.querySpecification.nodeConditions.HasAttrQualifiedNameValue;
import cz.muni.xmichalk.storage.EBundlePart;
import cz.muni.xmichalk.util.AttributeUtils;
import cz.muni.xmichalk.util.CpmUtils;

public class GetConnectors extends FindSubgraphsQuery<List<ConnectorData>> {
  public Boolean backward = null;
  // set by the traverser when the start node was reached via a connector of a neighbouring bundle
  public boolean enteredViaConnector = false;

  public GetConnectors() {
  }

  public GetConnectors(Boolean backward) {
    this.backward = backward;
  }

  public GetConnectors(Boolean backward, IFindableSubgraph fromSubgraphs) {
    this.backward = backward;
    this.fromSubgraphs = fromSubgraphs;
  }

  @Override
  public QueryResult<List<ConnectorData>> evaluate(QueryContext context) throws AccessDeniedException {
    String typeValueRegex = backward == null ? CPM_URI + "(backward|forward)Connector"
        : CPM_URI + (backward ? "backwardConnector" : "forwardConnector");

    IFindableSubgraph startsIn = enteredViaConnector && Boolean.TRUE.equals(backward)
        ? requireForwardConnector(context)
        : fromSubgraphs;
    IFindableSubgraph finder = new FittingNodes(new HasAttrQualifiedNameValue(ATTR_PROV_TYPE.getUri(), typeValueRegex),
        startsIn);

    return super.evaluate(context, finder);
  }

  // a bundle entered while traversing backward must be entered at its forward connector; any other start node
  // means the connector referencing it has a wrong id and part of this bundle would be silently skipped
  private IFindableSubgraph requireForwardConnector(QueryContext context) {
    HasAttrQualifiedNameValue isForwardConnector = new HasAttrQualifiedNameValue(ATTR_PROV_TYPE.getUri(),
        CPM_URI + "forwardConnector");

    return (graph, startNode) -> {
      if (startNode == null) {
        throw invalidEntryConnector(context, "does not exist");
      }
      if (!isForwardConnector.test(startNode)) {
        throw invalidEntryConnector(context, "is not a forwardConnector");
      }
      return fromSubgraphs.find(graph, startNode);
    };
  }

  private static IllegalArgumentException invalidEntryConnector(QueryContext context, String problem) {
    return new IllegalArgumentException("Invalid entry connector: " + context.startNodeId.getUri() + " in bundle "
        + context.documentId.getUri() + " " + problem + ". The backward connector referencing it likely uses a wrong id.");
  }

  @Override
  public EBundlePart decideRequiredBundlePart() {
    return EBundlePart.TraversalInformation;
  }

  @Override
  protected List<ConnectorData> transformResult(List<SubgraphWrapper> subgraphs) {
    if (subgraphs == null || subgraphs.isEmpty()) {
      return List.of();
    }

    Set<INode> connectors = subgraphs.stream().flatMap(subgraph -> subgraph.getNodes().stream())
        .collect(Collectors.toSet());

    if (connectors.isEmpty()) {
      return List.of();
    }
    return connectors.stream().map(this::transformToConnectorData).toList();
  }

  private ConnectorData transformToConnectorData(INode node) {
    ConnectorData connectorData = new ConnectorData();

    connectorData.id = new QualifiedNameData().from(node.getId());

    INode referencedConnector = CpmUtils.getGeneralConnectorId(node);
    QualifiedName referencedConnectorIdValue = referencedConnector != null
        ? referencedConnector.getId()
        : node.getId();
    connectorData.referencedConnectorId = new QualifiedNameData().from(referencedConnectorIdValue);

    Object referencedBundleIdValue = AttributeUtils.getAttributeValue(node, ATTR_REFERENCED_BUNDLE_ID);
    connectorData.referencedBundleId = referencedBundleIdValue == null
        ? null
        : new QualifiedNameData().from((org.openprovenance.prov.model.QualifiedName) referencedBundleIdValue);

    Object referencedMetaBundleIdValue = AttributeUtils.getAttributeValue(node, ATTR_REFERENCED_META_BUNDLE_ID);
    connectorData.referencedMetaBundleId = referencedMetaBundleIdValue == null
        ? null
        : new QualifiedNameData().from((org.openprovenance.prov.model.QualifiedName) referencedMetaBundleIdValue);

    Object provServiceUriValue = AttributeUtils.getAttributeValue(node, ATTR_PROVENANCE_SERVICE_URI);
    connectorData.provenanceServiceUri = provServiceUriValue == null
        ? null
        : ((String) provServiceUriValue);

    Object bundleHashValueObj = AttributeUtils.getAttributeValue(node, ATTR_REFERENCED_BUNDLE_HASH_VALUE);
    connectorData.referencedBundleHashValue = bundleHashValueObj != null
        ? ((String) bundleHashValueObj)
        : null;

    Object hashAlgObj = AttributeUtils.getAttributeValue(node, ATTR_HASH_ALG);
    connectorData.hashAlg = hashAlgObj != null
        ? ((String) hashAlgObj)
        : null;

    return connectorData;
  }
}
