/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

 package org.apache.myfaces.tobago.component;

 import org.apache.commons.beanutils.PropertyUtils;
 import org.apache.myfaces.tobago.component.Attributes;
 import org.apache.myfaces.tobago.component.SupportsMarkup;
 import org.apache.myfaces.tobago.component.SupportsRenderedPartially;
 import org.apache.myfaces.tobago.context.Markup;
 import org.apache.myfaces.tobago.el.ConstantMethodBinding;
 import org.apache.myfaces.tobago.internal.util.StringUtils;
 import org.apache.myfaces.tobago.util.ComponentUtils;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.el.ELException;
 import javax.el.ExpressionFactory;
 import javax.el.MethodExpression;
 import javax.el.ValueExpression;
 import javax.faces.FacesException;
 import javax.faces.component.ActionSource;
 import javax.faces.component.ActionSource2;
 import javax.faces.component.EditableValueHolder;
 import javax.faces.component.UIComponent;
 import javax.faces.component.ValueHolder;
 import javax.faces.convert.Converter;
 import javax.faces.event.MethodExpressionActionListener;
 import javax.faces.event.MethodExpressionValueChangeListener;
 import javax.faces.validator.MethodExpressionValidator;
 import javax.faces.view.facelets.ComponentHandler;
 import javax.faces.view.facelets.FaceletContext;
 import javax.faces.view.facelets.TagAttribute;
 import javax.faces.view.facelets.TagConfig;
 import javax.faces.view.facelets.TagException;
 import javax.faces.view.facelets.TagHandler;
 import java.beans.IntrospectionException;
 import java.beans.PropertyDescriptor;
 
 import org.apache.myfaces.tobago.event.SortActionEvent;
 import org.apache.myfaces.tobago.internal.component.AbstractUICommand;
 import org.apache.myfaces.tobago.internal.component.AbstractUISheet;
 import org.apache.myfaces.tobago.internal.util.StringUtils;
 import org.apache.myfaces.tobago.model.SheetState;
 import org.apache.myfaces.tobago.util.BeanComparator;
 import org.apache.myfaces.tobago.util.ValueExpressionComparator;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;
 
 import javax.el.ValueExpression;
 import javax.faces.component.UIColumn;
 import javax.faces.component.UICommand;
 import javax.faces.component.UIComponent;
 import javax.faces.component.UIInput;
 import javax.faces.component.UIOutput;
 import javax.faces.component.UISelectBoolean;
 import javax.faces.component.UISelectMany;
 import javax.faces.component.UISelectOne;
 import javax.faces.context.FacesContext;
 import javax.faces.model.DataModel;
 import java.util.ArrayList;
 import java.util.Arrays;
 import java.util.Collections;
 import java.util.Comparator;
 import java.util.List;
 
 //From http://grepcode.com/file_/repo1.maven.org/maven2/org.apache.myfaces.tobago/tobago-core/2.0.8/org/apache/myfaces/tobago/facelets/AttributeHandler.java/?v=source
 public final class AttributeHandler extends TagHandler {
 
     private static final Logger LOG = LoggerFactory.getLogger(org.apache.myfaces.tobago.facelets.AttributeHandler.class);
 
     private final TagAttribute name;
 
     private final TagAttribute value;
 
     private final TagAttribute mode;
 
     public AttributeHandler(final TagConfig config) {
         super(config);
         this.name = getRequiredAttribute(Attributes.NAME);
         this.value = getRequiredAttribute(Attributes.VALUE);
         this.mode = getAttribute(Attributes.MODE);
     }
 
     public void apply(final FaceletContext faceletContext, final UIComponent parent) throws ELException {
         if (parent == null) {
             throw new TagException(tag, "Parent UIComponent was null");
         }
 
         if (ComponentHandler.isNew(parent)) {
 
             if (mode != null) {
                 if ("isNotSet".equals(mode.getValue())) {
                     boolean result = false;
                     String expressionString = value.getValue();
                     if (!value.isLiteral()) {
                         while (isSimpleExpression(expressionString)) {
                             if (isMethodOrValueExpression(expressionString)) {
                                 final ValueExpression expression
                                         = faceletContext.getVariableMapper().resolveVariable(removeElParenthesis(expressionString));
                                 if (expression == null) {
                                     result = true;
                                     break;
                                 } else {
                                     expressionString = expression.getExpressionString();
                                 }
                             } else {
                                 result = false;
                                 break;
                             }
                         }
                     } else {
                         result = StringUtils.isEmpty(expressionString);
                     }
                     parent.getAttributes().put(name.getValue(), result);
                 } else if ("isSet".equals(mode.getValue())) {
                     boolean result = true;
                     String expressionString = value.getValue();
                     if (!value.isLiteral()) {
                         while (isSimpleExpression(expressionString)) {
                             if (isMethodOrValueExpression(expressionString)) {
                                 final ValueExpression expression
                                         = faceletContext.getVariableMapper().resolveVariable(removeElParenthesis(expressionString));
                                 if (expression == null) {
                                     result = false;
                                     break;
                                 } else {
                                     expressionString = expression.getExpressionString();
                                 }
                             } else {
                                 result = true;
                                 break;
                             }
                         }
                     } else {
                         result = StringUtils.isNotEmpty(expressionString);
                     }
                     parent.getAttributes().put(name.getValue(), result);
                 } else if ("action".equals(mode.getValue())) {
                     String expressionString = value.getValue();
                     while (isSimpleExpression(expressionString)) {
                         if (isMethodOrValueExpression(expressionString)) {
                             final ValueExpression expression
                                     = faceletContext.getVariableMapper().resolveVariable(removeElParenthesis(expressionString));
                             if (expression == null) {
                                 // when the action hasn't been set while using a composition.
                                 if (LOG.isDebugEnabled()) {
                                     LOG.debug("Variable can't be resolved: value='" + expressionString + "'");
                                 }
                                 expressionString = null;
                                 break;
                             } else {
                                 expressionString = expression.getExpressionString();
                             }
                         } else {
                             break;
                         }
                     }
                     if (expressionString != null) {
                         final ExpressionFactory expressionFactory = faceletContext.getExpressionFactory();
                         final MethodExpression action = new TagMethodExpression(value, expressionFactory.createMethodExpression(
                                 faceletContext, expressionString, String.class, ComponentUtils.ACTION_ARGS));
                         ((ActionSource2) parent).setActionExpression(action);
                     }
                 } else if ("actionListener".equals(mode.getValue())) {
                     String expressionString = value.getValue();
                     while (isSimpleExpression(expressionString)) {
                         if (isMethodOrValueExpression(expressionString)) {
                             final ValueExpression expression
                                     = faceletContext.getVariableMapper().resolveVariable(removeElParenthesis(expressionString));
                             if (expression == null) {
                                 if (LOG.isDebugEnabled()) {
                                     // when the action hasn't been set while using a composition.
                                     LOG.debug("Variable can't be resolved: value='" + expressionString + "'");
                                 }
                                 expressionString = null;
                                 break;
                             } else {
                                 expressionString = expression.getExpressionString();
                             }
                         } else {
                             LOG.warn("Only expressions are supported mode=actionListener value='" + expressionString + "'");
                             expressionString = null;
                             break;
                         }
                     }
                     if (expressionString != null) {
                         final ExpressionFactory expressionFactory = faceletContext.getExpressionFactory();
                         final MethodExpression actionListener
                                 = new TagMethodExpression(value, expressionFactory.createMethodExpression(
                                 faceletContext, expressionString, null, ComponentUtils.ACTION_LISTENER_ARGS));
                         ((ActionSource) parent).addActionListener(new MethodExpressionActionListener(actionListener));
                     }
                 } else if ("actionFromValue".equals(mode.getValue())) {
                     if (!value.isLiteral()) {
                         final String result = value.getValue(faceletContext);
                         parent.getAttributes().put(name.getValue(), new ConstantMethodBinding(result));
                     }
                 } else if ("valueIfSet".equals(mode.getValue())) {
                     String expressionString = value.getValue();
                     String lastExpressionString = null;
                     while (isMethodOrValueExpression(expressionString) && isSimpleExpression(expressionString)) {
                         final ValueExpression expression
                                 = faceletContext.getVariableMapper().resolveVariable(removeElParenthesis(expressionString));
                         if (expression != null) {
                             lastExpressionString = expressionString;
                             expressionString = expression.getExpressionString();
                         } else {
                             // restore last value
                             expressionString = lastExpressionString;
                             break;
                         }
                     }
                     if (expressionString != null) {
                         final String attributeName = name.getValue(faceletContext);
                         if (containsMethodOrValueExpression(expressionString)) {
                             final ValueExpression expression = value.getValueExpression(faceletContext, Object.class);
                             parent.setValueExpression(attributeName, expression);
                         } else {
                             final Object literalValue = getValue(faceletContext, parent, expressionString, attributeName);
                             parent.getAttributes().put(attributeName, literalValue);
                         }
                     }
                 } else {
                     throw new FacesException("Type " + mode + " not supported");
                 }
             } else {
 
                 final String nameValue = name.getValue(faceletContext);
                 if (Attributes.RENDERED.equals(nameValue)) {
                     if (value.isLiteral()) {
                         parent.setRendered(value.getBoolean(faceletContext));
                     } else {
                         parent.setValueExpression(nameValue, value.getValueExpression(faceletContext, Boolean.class));
                     }
                 } else if (Attributes.RENDERED_PARTIALLY.equals(nameValue)
                         && parent instanceof SupportsRenderedPartially) {
 
                     if (value.isLiteral()) {
                         final String[] components = ComponentUtils.splitList(value.getValue());
                         ((SupportsRenderedPartially) parent).setRenderedPartially(components);
                     } else {
                         parent.setValueExpression(nameValue, value.getValueExpression(faceletContext, Object.class));
                     }
                 } else if (Attributes.STYLE_CLASS.equals(nameValue)) {
                     // TODO expression
                     ComponentUtils.setStyleClasses(parent, value.getValue());
                 } else if (Attributes.MARKUP.equals(nameValue)) {
                     if (parent instanceof SupportsMarkup) {
                         if (value.isLiteral()) {
                             ((SupportsMarkup) parent).setMarkup(Markup.valueOf(value.getValue()));
                         } else {
                             final ValueExpression expression = value.getValueExpression(faceletContext, Object.class);
                             parent.setValueExpression(nameValue, expression);
                         }
                     } else {
                         LOG.error("Component is not instanceof SupportsMarkup. Instance is: " + parent.getClass().getName());
                     }
                 } else if (parent instanceof EditableValueHolder && Attributes.VALIDATOR.equals(nameValue)) {
                     final MethodExpression methodExpression
                             = getMethodExpression(faceletContext, null, ComponentUtils.VALIDATOR_ARGS);
                     if (methodExpression != null) {
                         ((EditableValueHolder) parent).addValidator(new MethodExpressionValidator(methodExpression));
                     }
                 } else if (parent instanceof EditableValueHolder
                         && Attributes.VALUE_CHANGE_LISTENER.equals(nameValue)) {
                     final MethodExpression methodExpression =
                             getMethodExpression(faceletContext, null, ComponentUtils.VALUE_CHANGE_LISTENER_ARGS);
                     if (methodExpression != null) {
                         ((EditableValueHolder) parent).addValueChangeListener(
                                 new MethodExpressionValueChangeListener(methodExpression));
                     }
                 } else if (parent instanceof ValueHolder && Attributes.CONVERTER.equals(nameValue)) {
                     setConverter(faceletContext, parent, nameValue);
                 } else if (parent instanceof ActionSource && Attributes.ACTION.equals(nameValue)) {
                     final MethodExpression action = getMethodExpression(faceletContext, String.class, ComponentUtils.ACTION_ARGS);
                     if (action != null) {
                         ((ActionSource2) parent).setActionExpression(action);
                     }
                 } else if (parent instanceof ActionSource && Attributes.ACTION_LISTENER.equals(nameValue)) {
                     final MethodExpression action
                             = getMethodExpression(faceletContext, null, ComponentUtils.ACTION_LISTENER_ARGS);
                     if (action != null) {
                         ((ActionSource) parent).addActionListener(new MethodExpressionActionListener(action));
                     }
                 } else if (!parent.getAttributes().containsKey(nameValue)) {
                     if (value.isLiteral()) {
                         parent.getAttributes().put(nameValue, value.getValue());
                     } else {
                         parent.setValueExpression(nameValue, value.getValueExpression(faceletContext, Object.class));
                     }
                 }
             }
         }
     }
 
     private boolean isMethodOrValueExpression(final String string) {
         return (string.startsWith("${") || string.startsWith("#{")) && string.endsWith("}");
     }
 
     private boolean containsMethodOrValueExpression(final String string) {
         return (string.contains("${") || string.contains("#{")) && string.contains("}");
     }
 
     private boolean isSimpleExpression(final String string) {
         return string.indexOf('.') < 0 && string.indexOf('[') < 0;
     }
 
     private String removeElParenthesis(final String string) {
         return string.substring(2, string.length() - 1);
     }
 
     private ValueExpression getExpression(final FaceletContext faceletContext) {
         final String myValue = removeElParenthesis(value.getValue());
         return faceletContext.getVariableMapper().resolveVariable(myValue);
     }
 
     private MethodExpression getMethodExpression(
             final FaceletContext faceletContext, final Class returnType, final Class[] args) {
         // in a composition may be we get the method expression string from the current variable mapper
         // the expression can be empty
         // in this case return nothing
         if (value.getValue().startsWith("${")) {
             final ValueExpression expression = getExpression(faceletContext);
             if (expression != null) {
                 final ExpressionFactory expressionFactory = faceletContext.getExpressionFactory();
                 return new TagMethodExpression(value, expressionFactory.createMethodExpression(faceletContext,
                         expression.getExpressionString(), returnType, args));
             } else {
                 return null;
             }
         } else {
             return value.getMethodExpression(faceletContext, returnType, args);
         }
     }
 
     private Object getValue(
             final FaceletContext faceletContext, final UIComponent parent, final String expressionString,
             final String attributeName) {
         Class type = Object.class;
         try {
             type = PropertyUtils.getReadMethod(
                     new PropertyDescriptor(attributeName, parent.getClass())).getReturnType();
         } catch (final IntrospectionException e) {
             LOG.warn("Can't determine expected type", e);
         }
         final ExpressionFactory expressionFactory = faceletContext.getExpressionFactory();
         final ValueExpression valueExpression = expressionFactory
                 .createValueExpression(faceletContext, expressionString, type);
         return valueExpression.getValue(faceletContext);
     }
 
     private void setConverter(final FaceletContext faceletContext, final UIComponent parent, final String nameValue) {
         // in a composition may be we get the converter expression string from the current variable mapper
         // the expression can be empty
         // in this case return nothing
         if (value.getValue().startsWith("${")) {
             final ValueExpression expression = getExpression(faceletContext);
             if (expression != null) {
                 setConverter(faceletContext, parent, nameValue, expression);
             }
         } else {
             setConverter(faceletContext, parent, nameValue, value.getValueExpression(faceletContext, Object.class));
         }
     }
 
     private void setConverter(
             final FaceletContext faceletContext, final UIComponent parent, final String nameValue,
            public void perform(final AbstractUISheet data) {
                Object value = data.getValue();
                if (value instanceof DataModel) {
                    value = ((DataModel) value).getWrappedData();
                }
                final FacesContext facesContext = FacesContext.getCurrentInstance();
                final SheetState sheetState = data.getSheetState(facesContext);

                final String sortedColumnId = sheetState.getSortedColumnId();
                if (LOG.isDebugEnabled()) {
                    LOG.debug("sorterId = '{}'", sortedColumnId);
                }

                if (sortedColumnId == null) {
                    // not to be sorted
                    return;
                }

                final UIColumn column = (UIColumn) data.findComponent(sortedColumnId);
                if (column == null) {
                    LOG.warn("No column to sort found, sorterId = '{}'", sortedColumnId);
                    return;
                }

                final Comparator actualComparator = getActualComparator(data, column, sheetState);
                if (actualComparator == null) {
                    return;
                }

                sortData(value, actualComparator);
                restoreSelectedRows(sheetState, value);
            }

            private Comparator getActualComparator(AbstractUISheet data, UIColumn column, SheetState sheetState) {
                final UIComponent child = getFirstSortableChild(column.getChildren());
                if (child == null) {
                    LOG.error("No sorting performed. No sortable child component found!");
                    unsetSortableAttribute(column);
                    return null;
                }

                final String attributeName = child instanceof AbstractUICommand ? Attributes.LABEL : Attributes.VALUE;
                final ValueExpression expression = child.getValueExpression(attributeName);
                if (expression == null) {
                    LOG.error("No sorting performed. No expression target found for sorting!");
                    unsetSortableAttribute(column);
                    return null;
                }

                final String var = data.getVar();
                if (var == null) {
                    LOG.error("No sorting performed. Property var of sheet is not set!");
                    unsetSortableAttribute(column);
                    return null;
                }

                final boolean descending = !sheetState.isAscending();
                if (isSimpleProperty(expression.getExpressionString())) {
                    final String sortProperty = expression.getExpressionString().substring(var.length() + 1);
                    return new BeanComparator(sortProperty, comparator, descending);
                } else {
                    return new ValueExpressionComparator(facesContext, var, expression, descending, comparator);
                }
            }

            private void sortData(Object value, Comparator comparator) {
                if (value instanceof List) {
                    Collections.sort((List) value, comparator);
                } else if (value instanceof Object[]) {
                    Arrays.sort((Object[]) value, comparator);
                }
            }

            private void restoreSelectedRows(SheetState sheetState, Object value) {
                if (sheetState.getSelectedRows().size() > 0) {
                    List<Object> selectedDataRows = new ArrayList<>(sheetState.getSelectedRows().size());
                    for (Integer index : sheetState.getSelectedRows()) {
                        Object dataRow = (value instanceof List) ? ((List) value).get(index) : ((Object[]) value)[index];
                        selectedDataRows.add(dataRow);
                    }
                    sheetState.getSelectedRows().clear();
                    for (Object dataRow : selectedDataRows) {
                        int index = getIndex(value, dataRow);
                        if (index >= 0) {
                            sheetState.getSelectedRows().add(index);
                        }
                    }
                }
            }

            private int getIndex(Object value, Object dataRow) {
                if (value instanceof List) {
                    return ((List) value).indexOf(dataRow);
                } else if (value instanceof Object[]) {
                    return Arrays.asList((Object[]) value).indexOf(dataRow);
                }
                return -1;
            }
