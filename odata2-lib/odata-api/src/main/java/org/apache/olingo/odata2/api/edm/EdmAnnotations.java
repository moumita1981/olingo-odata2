/*******************************************************************************
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 * 
 * http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 ******************************************************************************/
package org.apache.olingo.odata2.api.edm;

import java.util.List;

/**
 * @org.apache.olingo.odata2.DoNotImplement
 * EdmAnnotations holds all annotation attributes and elements for a specific CSDL element.
 * 
 */
public interface EdmAnnotations {

  /**
   * Get all annotation elements for the CSDL element
   * 
   * @return List of {@link EdmAnnotationElement}
   */
  List<EdmAnnotationElement> getAnnotationElements();

  /**
   * Get annotation element by full qualified name
   * 
   * @param name
   * @param namespace
   * @return String
   */

  EdmAnnotationElement getAnnotationElement(String name, String namespace);

  /**
   * Get all annotation attributes for the CSDL element
   * 
   * @return List of {@link EdmAnnotationAttribute}
   */
  List<EdmAnnotationAttribute> getAnnotationAttributes();

  /**
   * Get annotation attribute by full qualified name
   * 
   * @param name
   * @param namespace
   * @return String
   */
  EdmAnnotationAttribute getAnnotationAttribute(String name, String namespace);
}
