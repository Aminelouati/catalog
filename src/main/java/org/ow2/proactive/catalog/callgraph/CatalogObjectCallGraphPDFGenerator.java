/*
 * ProActive Parallel Suite(TM):
 * The Open Source library for parallel and distributed
 * Workflows & Scheduling, Orchestration, Cloud Automation
 * and Big Data Analysis on Enterprise Grids & Clouds.
 *
 * Copyright (c) 2007 - 2017 ActiveEon
 * Contact: contact@activeeon.com
 *
 * This library is free software: you can redistribute it and/or
 * modify it under the terms of the GNU Affero General Public License
 * as published by the Free Software Foundation: version 3 of
 * the License.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Affero General Public License for more details.
 *
 * You should have received a copy of the GNU Affero General Public License
 * along with this program. If not, see <http://www.gnu.org/licenses/>.
 *
 * If needed, contact us to obtain a release under GPL Version 2 or 3
 * or a different license than the AGPL.
 */
package org.ow2.proactive.catalog.callgraph;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.font.PDType0Font;
import org.ow2.proactive.catalog.dto.CatalogObjectMetadata;
import org.ow2.proactive.catalog.dto.Metadata;
import org.ow2.proactive.catalog.report.HeadersBuilder;
import org.ow2.proactive.catalog.service.CatalogObjectService;
import org.ow2.proactive.catalog.service.exception.PDFGenerationException;
import org.ow2.proactive.catalog.util.SeparatorUtility;
import org.ow2.proactive.catalog.util.parser.WorkflowParser;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import be.quodlibet.boxable.BaseTable;
import be.quodlibet.boxable.utils.FontUtils;


/**
 * @author ActiveEon Team
 * @since 2019-03-25
 */

@Component
public class CatalogObjectCallGraphPDFGenerator {

    private static final float MARGIN = 10;

    private static final String MAIN_TITLE = "ProActive Call Graph Report";

    @Value("${pa.catalog.pdf.report.ttf.font.path}")
    private String ttfFontPath;

    @Value("${pa.catalog.pdf.report.ttf.font.bold.path}")
    private String ttfFontBoldPath;

    @Value("${pa.catalog.pdf.report.ttf.font.italic.path}")
    private String ttfFontItalicPath;

    @Value("${pa.catalog.pdf.report.ttf.font.bold.italic.path}")
    private String ttfFontBoldItalicPath;

    @Autowired
    private HeadersBuilder headersBuilder;

    @Autowired
    private SeparatorUtility separatorUtility;

    @Autowired
    private TableCallGraphsBuilder tableGraphPathBuilder;

    public byte[] generatePdfImage(List<CatalogObjectMetadata> catalogObjectMetadataList, Optional<String> kind,
            Optional<String> contentType, CatalogObjectService catalogObjectService) {

        try (ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
                PDDocument doc = new PDDocument()) {

            // Build call graph
            CallGraphHolder globalCallGraph = buildCatalogCallGraph(catalogObjectMetadataList, catalogObjectService);

            //Load font for all languages
            setFontToUse(doc);

            // Initialize Document
            PDPage page = addNewPage(doc);

            // Initialize table
            BaseTable table = initializeTable(doc, MARGIN, page);

            // Create Header row
            headersBuilder.createMainHeader(table, MAIN_TITLE);

            // Create Header row

            headersBuilder.createInfoHeader(table,
                                            extractBucketSet(globalCallGraph),
                                            extractObjectSet(globalCallGraph),
                                            kind,
                                            contentType);

            //Create graph path table
            tableGraphPathBuilder.buildCallGraphsTable(globalCallGraph, table);

            table.draw();

            doc.save(byteArrayOutputStream);

            return byteArrayOutputStream.toByteArray();

        } catch (Exception e) {
            throw new PDFGenerationException(e);
        }
    }

    private List<String> collectDependsOnCatalogObjects(CatalogObjectMetadata catalogObjectMetadata) {
        return catalogObjectMetadata.getMetadataList()
                                    .stream()
                                    .filter(metadata -> metadata.getLabel()
                                                                .equals(WorkflowParser.ATTRIBUTE_DEPENDS_ON_LABEL))
                                    .map(Metadata::getKey)
                                    .collect(Collectors.toList());

    }

    private CallGraphHolder buildCatalogCallGraph(List<CatalogObjectMetadata> catalogObjectMetadataList,
            CatalogObjectService catalogObjectService) {

        CallGraphHolder callGraphHolder = new CallGraphHolder();
        for (CatalogObjectMetadata catalogObjectMetadata : catalogObjectMetadataList) {
            List<String> dependsOnCatalogObjects = collectDependsOnCatalogObjects(catalogObjectMetadata);
            if (!dependsOnCatalogObjects.isEmpty()) {
                GraphNode callingCatalogObject = callGraphHolder.addNode(catalogObjectMetadata.getBucketName(),
                                                                         catalogObjectMetadata.getName(),
                                                                         catalogObjectMetadata.getKind(),
                                                                         true);
                String bucketName;
                String objectName;
                boolean isCatalogObjectExist;
                String objectKind;
                GraphNode calledCatalogObject;
                for (String dependsOnCatalogObject : dependsOnCatalogObjects) {
                    bucketName = separatorUtility.getSplitBySeparator(dependsOnCatalogObject).get(0);
                    objectName = separatorUtility.getSplitBySeparator(dependsOnCatalogObject).get(1);
                    isCatalogObjectExist = catalogObjectService.isDependsOnObjectExistInCatalog(bucketName,
                                                                                                objectName,
                                                                                                WorkflowParser.LATEST_VERSION);
                    objectKind = isCatalogObjectExist ? catalogObjectService.getCatalogObjectMetadata(bucketName,
                                                                                                      objectName)
                                                                            .getKind()
                                                      : "N/A";
                    calledCatalogObject = callGraphHolder.addNode(bucketName,
                                                                  objectName,
                                                                  objectKind,
                                                                  isCatalogObjectExist);
                    callGraphHolder.addDependsOnEdge(callingCatalogObject, calledCatalogObject);
                }
            }
        }
        return callGraphHolder;
    }

    private void setFontToUse(PDDocument doc) throws IOException {
        FontUtils.setSansFontsAsDefault(doc);
        addFontTypeIfFileExists(doc, ttfFontPath, "font");
        addFontTypeIfFileExists(doc, ttfFontBoldPath, "fontBold");
        addFontTypeIfFileExists(doc, ttfFontItalicPath, "fontItalic");
        addFontTypeIfFileExists(doc, ttfFontBoldItalicPath, "fontBoldItalic");
    }

    private void addFontTypeIfFileExists(PDDocument doc, String path, String fontType) throws IOException {
        if (!StringUtils.isEmpty(path) && new File(path).exists()) {
            FontUtils.getDefaultfonts().put(fontType, PDType0Font.load(doc, new File(path)));
        }
    }

    private BaseTable initializeTable(PDDocument doc, float margin, PDPage page) throws IOException {
        float tableWidth = page.getMediaBox().getWidth() - (2 * margin);
        float yStartNewPage = page.getMediaBox().getHeight() - (2 * margin);
        boolean drawContent = true;
        boolean drawLines = true;
        float yStart = yStartNewPage;
        float bottomMargin = 70;
        return new BaseTable(yStart,
                             yStartNewPage,
                             bottomMargin,
                             tableWidth,
                             margin,
                             doc,
                             page,
                             drawLines,
                             drawContent);
    }

    private PDPage addNewPage(PDDocument doc) {
        PDPage page = new PDPage();
        doc.addPage(page);
        return page;
    }

    private Set<String> extractBucketSet(CallGraphHolder callGraphHolder) {
        Set<String> bucketSet = new HashSet<>();
        callGraphHolder.nodeSet().forEach(graphNode -> bucketSet.add(graphNode.getBucketName()));
        return bucketSet;

    }

    private Set<String> extractObjectSet(CallGraphHolder callGraphHolder) {
        Set<String> objectSet = new HashSet<>();
        callGraphHolder.nodeSet().forEach(graphNode -> objectSet.add(graphNode.getObjectName()));
        return objectSet;

    }
}
