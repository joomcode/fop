/*
 * $Id$
 * ============================================================================
 *                    The Apache Software License, Version 1.1
 * ============================================================================
 *
 * Copyright (C) 1999-2003 The Apache Software Foundation. All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without modifica-
 * tion, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice,
 *    this list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. The end-user documentation included with the redistribution, if any, must
 *    include the following acknowledgment: "This product includes software
 *    developed by the Apache Software Foundation (http://www.apache.org/)."
 *    Alternately, this acknowledgment may appear in the software itself, if
 *    and wherever such third-party acknowledgments normally appear.
 *
 * 4. The names "FOP" and "Apache Software Foundation" must not be used to
 *    endorse or promote products derived from this software without prior
 *    written permission. For written permission, please contact
 *    apache@apache.org.
 *
 * 5. Products derived from this software may not be called "Apache", nor may
 *    "Apache" appear in their name, without prior written permission of the
 *    Apache Software Foundation.
 *
 * THIS SOFTWARE IS PROVIDED ``AS IS'' AND ANY EXPRESSED OR IMPLIED WARRANTIES,
 * INCLUDING, BUT NOT LIMITED TO, THE IMPLIED WARRANTIES OF MERCHANTABILITY AND
 * FITNESS FOR A PARTICULAR PURPOSE ARE DISCLAIMED. IN NO EVENT SHALL THE
 * APACHE SOFTWARE FOUNDATION OR ITS CONTRIBUTORS BE LIABLE FOR ANY DIRECT,
 * INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES (INCLU-
 * DING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES; LOSS
 * OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND ON
 * ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 * ============================================================================
 *
 * This software consists of voluntary contributions made by many individuals
 * on behalf of the Apache Software Foundation and was originally created by
 * James Tauber <jtauber@jtauber.com>. For more information on the Apache
 * Software Foundation, please see <http://www.apache.org/>.
 */

/*
 * This file is part of the RTF library of the FOP project, which was originally
 * created by Bertrand Delacretaz <bdelacretaz@codeconsult.ch> and by other
 * contributors to the jfor project (www.jfor.org), who agreed to donate jfor to
 * the FOP project.
 */

package org.apache.fop.rtf.rtflib.rtfdoc;

import java.io.Writer;
import java.io.IOException;
import java.util.Iterator;
import org.apache.fop.rtf.rtflib.interfaces.ITableColumnsInfo;

/**  A cell in an RTF table, container for paragraphs, lists, etc.
 *  @author Bertrand Delacretaz bdelacretaz@codeconsult.ch
 */

public class RtfTableCell
extends RtfContainer
implements IRtfParagraphContainer, IRtfListContainer, IRtfTableContainer,
       IRtfExternalGraphicContainer {
    private RtfParagraph paragraph;
    private RtfList list;
    private RtfTable table;
    private RtfExternalGraphic externalGraphic;
    private final RtfTableRow parentRow;
    private boolean setCenter;
    private boolean setRight;
    private int id;

    /** default cell width (in twips ??) */
    public static final int DEFAULT_CELL_WIDTH = 2000;

    /** cell width in twips */
    private int cellWidth;
    private int widthOffset;

    /** cell merging has three states */
    private int vMerge = NO_MERGE;
    private int hMerge = NO_MERGE;

    /** cell merging: this cell is not merged */
    public static final int NO_MERGE = 0;

    /** cell merging: this cell is the start of a range of merged cells */
    public static final int MERGE_START = 1;

    /** cell merging: this cell is part of (but not the start of) a range of merged cells */
    public static final int MERGE_WITH_PREVIOUS = 2;

    /** Create an RTF element as a child of given container */
    RtfTableCell(RtfTableRow parent, Writer w, int cellWidth, int idNum) throws IOException {
        super(parent, w);
        id = idNum;
        parentRow = parent;
        this.cellWidth = cellWidth;
        setCenter = setRight = false;

    }

    /** Create an RTF element as a child of given container */
    RtfTableCell(RtfTableRow parent, Writer w, int cellWidth, RtfAttributes attrs,
            int idNum) throws IOException {
        super(parent, w, attrs);
        id = idNum;
        parentRow = parent;
        this.cellWidth = cellWidth;

    /** Added by Boris Poudérous on 07/22/2002 in order to process
     *  number-columns-spanned attribute */
    // If the cell is spanned horizontally
    if (attrs.getValue("number-columns-spanned") != null) {
        // Start horizontal merge
        this.setHMerge(MERGE_START);

        // Get the number of columns spanned
        int nbMergedCells = ((Integer)attrs.getValue("number-columns-spanned")).intValue();

        if (parent.parent instanceof RtfTable) {
            // Get the context of the current table in order to get the width of each column
            ITableColumnsInfo tableColumnsInfo =
                    ((RtfTable)parent.parent).getITableColumnsInfo();
            tableColumnsInfo.selectFirstColumn();

            // Reach the column index in table context corresponding to the current column cell
            // id is the index of the current cell (it begins at 1)
            // getColumnIndex() is the index of the current column in table context (it begins at 0)
            //  => so we must widthdraw 1 when comparing these two variables.
            while ((this.id - 1) != tableColumnsInfo.getColumnIndex()) {
               tableColumnsInfo.selectNextColumn();
            }

            // We widthdraw one cell because the first cell is already created
            // (it's the current cell) !
            int i = nbMergedCells - 1;
            while (i > 0) {
                tableColumnsInfo.selectNextColumn();
                // Added by Normand Masse
                // Pass in the current cell's attributes so the 'merged' cell has the
                // same display attributes.
                parent.newTableCellMergedHorizontally((int)tableColumnsInfo.getColumnWidth(),
                        attrs);

                i--;
              }
          }
      }
      /** - end - */
    }

    /**
     * Start a new paragraph after closing current current paragraph, list and table
     * @param attrs attributes of new RtfParagraph
     * @return new RtfParagraph object
     * @throws IOException for I/O problems
     */
    public RtfParagraph newParagraph(RtfAttributes attrs) throws IOException {
        closeAll();

        // in tables, RtfParagraph must have the intbl attribute
        if (attrs == null) {
            attrs = new RtfAttributes();
        }
        attrs.set("intbl");

        paragraph = new RtfParagraph(this, writer, attrs);

        if (paragraph.attrib.isSet("qc")) {
            setCenter = true;
            attrs.set("qc");
        } else if (paragraph.attrib.isSet("qr")) {
            setRight = true;
            attrs.set("qr");
        } else {
            attrs.set("ql");
        }
        attrs.set("intbl");


        //lines modified by Chris Scott, Westinghouse
        return paragraph;
    }

    /**
     * Start a new external graphic after closing current paragraph, list and table
     * @throws IOException for I/O problems
     * @return new RtfExternalGraphic object
     */
    public RtfExternalGraphic newImage() throws IOException {
        closeAll();
        externalGraphic = new RtfExternalGraphic(this, writer);
        return externalGraphic;
    }

    /**
     * Start a new paragraph with default attributes after closing current
     * paragraph, list and table
     * @return new RtfParagraph object
     * @throws IOException for I/O problems
     */
    public RtfParagraph newParagraph() throws IOException {
        return newParagraph(null);
    }

    /**
     * Start a new list after closing current paragraph, list and table
     * @param attrib attributes for new RtfList
     * @return new RtfList object
     * @throws IOException for I/O problems
     */
    public RtfList newList(RtfAttributes attrib) throws IOException {
        closeAll();
        list = new RtfList(this, writer, attrib);
        return list;
    }

    /**
     * Start a new nested table after closing current paragraph, list and table
     * @param tc table column info for new RtfTable
     * @return new RtfTable object
     * @throws IOException for I/O problems
     */
    public RtfTable newTable(ITableColumnsInfo tc) throws IOException {
        closeAll();
        table = new RtfTable(this, writer, tc);
        return table;
    }

    /**
     * Start a new nested table after closing current paragraph, list and table
     * @param attrs attributes of new RtfTable
     * @param tc table column info for new RtfTable
     * @return new RtfTable object
     * @throws IOException for I/O problems
     */
    // Modified by Boris Poudérous on 07/22/2002
    public RtfTable newTable(RtfAttributes attrs, ITableColumnsInfo tc) throws IOException {
        closeAll();
        table = new RtfTable(this, writer, attrs, tc); // Added tc Boris Poudérous 07/22/2002
        return table;
    }

    /** used by RtfTableRow to write the <celldef> cell definition control words
     *  @param widthOffset sum of the widths of preceeding cells in same row
     *  @return widthOffset + width of this cell
     */
    int writeCellDef(int widthOffset) throws IOException {
        this.widthOffset = widthOffset;

        // vertical cell merge codes
        if (vMerge == MERGE_START) {
            writeControlWord("clvmgf");
        } else if (vMerge == MERGE_WITH_PREVIOUS) {
            writeControlWord("clvmrg");
        }

        // horizontal cell merge codes
        if (hMerge == MERGE_START) {
            writeControlWord("clmgf");
        } else if (hMerge == MERGE_WITH_PREVIOUS) {
            writeControlWord("clmrg");
        }

        /**
         * Added by Boris POUDEROUS on 2002/06/26
         */
        // Cell background color processing :
        writeAttributes (attrib, ITableAttributes.CELL_COLOR);
        /** - end - */

        writeAttributes (attrib, ITableAttributes.ATTRIB_CELL_PADDING);
        writeAttributes (attrib, ITableAttributes.CELL_BORDER);
        writeAttributes (attrib, BorderAttributesConverter.BORDERS);

        // cell width
        final int xPos = widthOffset + this.cellWidth;

        //these lines added by Chris Scott, Westinghouse
        //some attributes need to be writting before opening block
        if (setCenter) {
            writeControlWord("qc");
        } else if (setRight) {
            writeControlWord("qr");
        } else {
            writeControlWord("ql");
        }

        writeControlWord("cellx" + xPos);

        writeControlWord("ql");

        return xPos;

    }

    /**
     * The "cell" control word marks the end of a cell
     * @throws IOException for I/O problems
     */
    protected void writeRtfSuffix() throws IOException {
        // word97 hangs if cell does not contain at least one "par" control word
        // TODO this is what causes the extra spaces in nested table of test
        //      004-spacing-in-tables.fo,
        // but if is not here we generate invalid RTF for word97

        if (setCenter) {
            writeControlWord("qc");
        } else if (setRight) {
            writeControlWord("qr");
        } else {
            writeControlWord("ql");
        }



        if (!containsText()) {
            writeControlWord("intbl");

            //R.Marra this create useless paragraph
            //Seem working into Word97 with the "intbl" only
//            writeControlWord("par");
        }

        writeControlWord("cell");
    }


    //modified by Chris Scott, Westinghouse
    private void closeCurrentParagraph() throws IOException {
        if (paragraph != null) {
            paragraph.close();
        }
    }

    private void closeCurrentList() throws IOException {
        if (list != null) {
            list.close();
        }
    }

    private void closeCurrentTable() throws IOException {
        if (table != null) {
            table.close();
        }
    }

    private void closeCurrentExternalGraphic() throws IOException {
        if (externalGraphic != null) {
            externalGraphic.close();
        }
    }

    private void closeAll()
    throws IOException {
        closeCurrentTable();
        closeCurrentParagraph();
        closeCurrentList();
        closeCurrentExternalGraphic();
    }

    /**
     * @param mergeStatus vertical cell merging status to set
     */
    public void setVMerge(int mergeStatus) { this.vMerge = mergeStatus; }

    /**
     * @return vertical cell merging status
     */
    public int getVMerge() { return this.vMerge; }

    /**
     * Set horizontal cell merging status
     * @param mergeStatus mergeStatus to set
     */
    public void setHMerge(int mergeStatus) {
        this.hMerge = mergeStatus;
    }

    /**
     * @return horizontal cell merging status
     */
    public int getHMerge() {
        return this.hMerge;
    }

    /** get cell width */
    int getCellWidth() { return this.cellWidth; }

    /**
     * Overridden so that nested tables cause extra rows to be added after the row
     * that contains this cell
     * disabled for V0.3 - nested table support is not done yet
     * @throws IOException for I/O problems
     */
    protected void writeRtfContent()
    throws IOException {
        int extraRowIndex = 0;
        RtfTableCell extraCell = null;

        for (Iterator it = getChildren().iterator(); it.hasNext();) {
            final RtfElement e = (RtfElement)it.next();
            if (e instanceof RtfTable) {
                // nested table - render its cells in supplementary rows after current row,
                // and put the remaining content of this cell in a new cell after nested table
                // Line added by Boris Poudérous
        parentRow.getExtraRowSet().setParentITableColumnsInfo(
                ((RtfTable)this.getParentOfClass(e.getClass())).getITableColumnsInfo());
        extraRowIndex = parentRow.getExtraRowSet().addTable((RtfTable)e,
                extraRowIndex, widthOffset);
                // Boris Poudérous added the passing of the current cell
                // attributes to the new cells (in order not to have cell without
                // border for example)
        extraCell = parentRow.getExtraRowSet().createExtraCell(extraRowIndex,
                widthOffset, this.getCellWidth(), attrib);
                extraRowIndex++;

            } else if (extraCell != null) {
                // we are after a nested table, add elements to the extra cell created for them
                extraCell.addChild(e);

            } else {
                // before a nested table, normal rendering
                e.writeRtf();
            }
        }
    }

    /**
     * A table cell always contains "useful" content, as it is here to take some
     * space in a row.
     * Use containsText() to find out if there is really some useful content in the cell.
     * TODO: containsText could use the original isEmpty implementation?
     * @return false (always)
     */
    public boolean isEmpty() {
        return false;
    }

    /** true if the "par" control word must be written for given RtfParagraph
     *  (which is not the case for the last non-empty paragraph of the cell)
     */
    boolean paragraphNeedsPar(RtfParagraph p) {
        // true if there is at least one non-empty paragraph after p in our children
        boolean pFound = false;
        boolean result = false;
        for (Iterator it = getChildren().iterator(); it.hasNext();) {
            final Object o = it.next();
            if (!pFound) {
                // set pFound when p is found in the list
                pFound =  (o == p);
            } else {
                if (o instanceof RtfParagraph) {
                    final RtfParagraph p2 = (RtfParagraph)o;
                    if (!p2.isEmpty()) {
                        // found a non-empty paragraph after p
                        result = true;
                        break;
                    }
                } else if (o instanceof RtfTable) {
                    break;
                }
            }
        }
        return result;
    }
}
