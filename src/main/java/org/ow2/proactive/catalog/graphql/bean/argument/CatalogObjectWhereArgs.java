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
package org.ow2.proactive.catalog.graphql.bean.argument;

import java.util.List;

import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;


/**
 * @author ActiveEon Team
 * @since 12/06/2017
 */
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Data
public class CatalogObjectWhereArgs {

    @JsonProperty("AND")
    private List<CatalogObjectWhereArgs> andArg;

    @JsonProperty("OR")
    private List<CatalogObjectWhereArgs> orArg;

    private CatalogObjectMetadataArgs metadataArg;

    private CatalogObjectBucketNameWhereArgs bucketNameArg;

    private CatalogObjectKindWhereArgs kindArg;

    private CatalogObjectContentTypeWhereArgs contentTypeArg;

    private CatalogObjectNameWhereArgs nameArg;

}
