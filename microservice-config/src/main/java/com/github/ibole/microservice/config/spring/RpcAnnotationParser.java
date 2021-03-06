package com.github.ibole.microservice.config.spring;

import com.github.ibole.microservice.config.spring.support.RpcAnnotation;

import com.google.common.base.Strings;

import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.support.RootBeanDefinition;
import org.springframework.beans.factory.xml.BeanDefinitionParser;
import org.springframework.beans.factory.xml.ParserContext;
import org.w3c.dom.Element;

/*********************************************************************************************.
 * 
 * 
 * <p>Copyright 2016, iBole Inc. All rights reserved.
 * 
 * <p></p>
 *********************************************************************************************/


/**
 * Rpc Annotation Parser for service reference.
 * @author bwang
 *
 */
public class RpcAnnotationParser implements BeanDefinitionParser {
  
  public BeanDefinition parse(Element element, ParserContext parserContext) {
    String id = element.getAttribute("id");
    if (Strings.isNullOrEmpty(id)) {
      String name = element.getAttribute("name");
      if (!Strings.isNullOrEmpty(name)) {
        id = name;
      } else {
        id = RpcAnnotation.class.getName();
      }
    }
    if (parserContext.getRegistry().containsBeanDefinition(id)) {
      throw new IllegalStateException("Duplicate spring bean id " + id);
    }
    String annotationPackage = element.getAttribute("package");
    RootBeanDefinition beanDefinition = new RootBeanDefinition();
    beanDefinition.setBeanClass(RpcAnnotation.class);
    beanDefinition.setLazyInit(false);
    if (!Strings.isNullOrEmpty(annotationPackage)) {
      beanDefinition.getPropertyValues().addPropertyValue("annotationPackage", annotationPackage);
    }
    parserContext.getRegistry().registerBeanDefinition(id, beanDefinition);
    return beanDefinition;
  }
}
