/*
 * Copyright (c) 2011 Google Inc.
 *
 * All rights reserved. This program and the accompanying materials are made available under the terms of the Eclipse
 * Public License v1.0 which accompanies this distribution, and is available at
 *
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.protobuf.scoping;

import static com.google.common.collect.Sets.newHashSet;
import static java.util.Collections.emptySet;
import static org.eclipse.xtext.resource.EObjectDescription.create;

import java.util.*;

import org.eclipse.emf.ecore.EObject;
import org.eclipse.xtext.naming.QualifiedName;
import org.eclipse.xtext.resource.IEObjectDescription;

import com.google.eclipse.protobuf.naming.*;
import com.google.eclipse.protobuf.protobuf.*;
import com.google.eclipse.protobuf.protobuf.Package;
import com.google.inject.Inject;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
class ComplexTypeFinderStrategy implements ModelElementFinder.FinderStrategy {
  @Inject private PackageIntersectionDescriptions packageIntersectionDescriptions;
  @Inject private ProtoDescriptorProvider descriptorProvider;
  @Inject private LocalNamesProvider localNamesProvider;
  @Inject private NormalNamingStrategy namingStrategy;
  @Inject private QualifiedNameDescriptions qualifiedNamesDescriptions;

  @Override public Collection<IEObjectDescription> imported(Package fromImporter, Package fromImported, Object target,
      Object criteria) {
    if (!isInstance(target, criteria)) {
      return emptySet();
    }
    Set<IEObjectDescription> descriptions = newHashSet();
    EObject e = (EObject) target;
    descriptions.addAll(qualifiedNamesDescriptions.qualifiedNames(e, namingStrategy));
    descriptions.addAll(packageIntersectionDescriptions.intersection(fromImporter, fromImported, e));
    return descriptions;
  }

  @Override public Collection<IEObjectDescription> inDescriptor(Import anImport, Object criteria) {
    Set<IEObjectDescription> descriptions = newHashSet();
    ProtoDescriptor descriptor = descriptorProvider.descriptor(anImport.getImportURI());
    for (ComplexType type : descriptor.allTypes()) {
      if (!isInstance(type, criteria)) {
        continue;
      }
      descriptions.addAll(qualifiedNamesDescriptions.qualifiedNames(type, namingStrategy));
    }
    return descriptions;
  }

  @Override public Collection<IEObjectDescription> local(Object target, Object criteria, int level) {
    if (!isInstance(target, criteria)) {
      return emptySet();
    }
    EObject e = (EObject) target;
    Set<IEObjectDescription> descriptions = newHashSet();
    List<QualifiedName> names = localNamesProvider.localNames(e, namingStrategy);
    int nameCount = names.size();
    for (int i = level; i < nameCount; i++) {
      descriptions.add(create(names.get(i), e));
    }
    descriptions.addAll(qualifiedNamesDescriptions.qualifiedNames(e, namingStrategy));
    return descriptions;
  }

  private boolean isInstance(Object target, Object criteria) {
    Class<?> targetType = targetTypeFrom(criteria);
    return targetType.isInstance(target);
  }

  private Class<?> targetTypeFrom(Object criteria) {
    if (criteria instanceof Class<?>) {
      return (Class<?>) criteria;
    }
    throw new IllegalArgumentException("Search criteria should be Class<?>");
  }
}