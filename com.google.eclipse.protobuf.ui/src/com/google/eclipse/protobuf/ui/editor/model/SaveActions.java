/*
 * Copyright (c) 2011 Google Inc.
 * 
 * All rights reserved. This program and the accompanying materials are made
 * available under the terms of the Eclipse Public License v1.0 which
 * accompanies this distribution, and is available at
 * 
 * http://www.eclipse.org/legal/epl-v10.html
 */
package com.google.eclipse.protobuf.ui.editor.model;

import static java.lang.Character.isWhitespace;
import static org.eclipse.jface.text.IDocumentExtension3.DEFAULT_PARTITIONING;
import static org.eclipse.jface.text.TextUtilities.getPartition;

import com.google.eclipse.protobuf.ui.preferences.pages.editor.save.*;
import com.google.inject.*;

import org.apache.log4j.Logger;
import org.eclipse.jface.text.*;
import org.eclipse.text.edits.*;

/**
 * @author alruiz@google.com (Alex Ruiz)
 */
@Singleton
class SaveActions {

  @Inject private SaveActionsPreferencesFactory preferencesFactory;
  
  private static Logger logger = Logger.getLogger(SaveActions.class);
  
  TextEdit createSaveAction(IDocument document, IRegion[] changedRegions) {
    SaveActionsPreferences preferences = preferencesFactory.preferences();
    if (!preferences.shouldRemoveTrailingSpace()) return null;
    TextEdit rootEdit = null;
    try {
      for (IRegion region : changedRegions) {
        int lastLine = document.getLineOfOffset(region.getOffset() + region.getLength());
        for (int line = firstLine(region, document); line <= lastLine; line++) {
          IRegion lineRegion = document.getLineInformation(line);
          if (lineRegion.getLength() == 0) continue;
          int lineStart = lineRegion.getOffset();
          int lineEnd = lineStart + lineRegion.getLength();
          int charPos = rightMostNonWhitespaceChar(document, lineStart, lineEnd);
          if (charPos >= lineEnd) continue;
          // check partition - don't remove whitespace inside strings
          ITypedRegion partition = getPartition(document, DEFAULT_PARTITIONING, charPos, false);
          if ("__string".equals(partition.getType())) continue;
          if (rootEdit == null) rootEdit = new MultiTextEdit();
          rootEdit.addChild(new DeleteEdit(charPos, lineEnd - charPos));
        }
      }
    } catch (BadLocationException e) {
      logger.warn("Unable to create save actions", e);
    }
    return rootEdit;
  }

  private int firstLine(IRegion region, IDocument document) throws BadLocationException {
    return document.getLineOfOffset(region.getOffset());
  }

  private int rightMostNonWhitespaceChar(IDocument document, int lineStart, int lineEnd) throws BadLocationException {
    int charPos = lineEnd - 1;
    while (charPos >= lineStart && isWhitespace(document.getChar(charPos))) {
      charPos--;
    }
    return ++charPos;
  }
}