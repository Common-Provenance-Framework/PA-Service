package cz.muni.xmichalk.querySpecification.nodeConditions;

import cz.muni.fi.cpm.model.INode;
import cz.muni.xmichalk.querySpecification.ICondition;
import cz.muni.xmichalk.util.AttributeUtils;

public class HasAttrStringValue implements ICondition<INode> {
  public String attributeNameUri;
  public String valueRegex;

  public HasAttrStringValue() {
  }

  public HasAttrStringValue(String attributeNameUri, String valueRegex) {
    this.attributeNameUri = attributeNameUri;
    this.valueRegex = valueRegex;
  }

  @Override
  public boolean test(INode node) {
    if (attributeNameUri == null) {
      throw new IllegalStateException(
          "Value of attributeNameUri cannot be null in " + this.getClass().getSimpleName());
    }
    if (valueRegex == null) {
      throw new IllegalStateException(
          "valueRegex must be non-null in " + this.getClass().getSimpleName());
    }

    try {
      return AttributeUtils.hasAttributeTargetValue(
          node, attributeNameUri, String.class, (value) -> {
            return valueRegex == null || value.matches(valueRegex);
          });
    } catch (Exception e) {
      return false;
    }
  }

}
