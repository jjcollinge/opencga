/*
 * Copyright 2015-2016 OpenCB
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.opencb.opencga.app.cli.main.executors.catalog;


import org.apache.commons.lang3.StringUtils;
import org.opencb.commons.datastore.core.ObjectMap;
import org.opencb.commons.datastore.core.Query;
import org.opencb.commons.datastore.core.QueryOptions;
import org.opencb.commons.datastore.core.QueryResponse;
import org.opencb.opencga.analysis.storage.variant.CatalogVariantDBAdaptor;
import org.opencb.opencga.app.cli.main.OpencgaCommandExecutor;
import org.opencb.opencga.app.cli.main.executors.catalog.commons.AclCommandExecutor;
import org.opencb.opencga.app.cli.main.options.catalog.StudyCommandOptions;
import org.opencb.opencga.catalog.db.api.FileDBAdaptor;
import org.opencb.opencga.catalog.db.api.JobDBAdaptor;
import org.opencb.opencga.catalog.db.api.SampleDBAdaptor;
import org.opencb.opencga.catalog.db.api.StudyDBAdaptor;
import org.opencb.opencga.catalog.exceptions.CatalogException;
import org.opencb.opencga.catalog.models.File;
import org.opencb.opencga.catalog.models.Job;
import org.opencb.opencga.catalog.models.Sample;
import org.opencb.opencga.catalog.models.Study;
import org.opencb.opencga.catalog.models.acls.permissions.StudyAclEntry;
import org.opencb.opencga.catalog.models.summaries.StudySummary;
import org.opencb.opencga.client.rest.catalog.StudyClient;

import java.io.IOException;

/**
 * Created by imedina on 03/06/16.
 */
public class StudiesCommandExecutor extends OpencgaCommandExecutor {
    // TODO: Add include/exclude/skip/... (queryOptions) to the client calls !!!!

    private StudyCommandOptions studiesCommandOptions;
    private AclCommandExecutor<Study, StudyAclEntry> aclCommandExecutor;

    public StudiesCommandExecutor(StudyCommandOptions studiesCommandOptions) {
        super(studiesCommandOptions.commonCommandOptions);
        this.studiesCommandOptions = studiesCommandOptions;
        this.aclCommandExecutor = new AclCommandExecutor<>();
    }


    @Override
    public void execute() throws Exception {

        String subCommandString = getParsedSubCommand(studiesCommandOptions.jCommander);
        QueryResponse queryResponse = null;
        logger.debug("Executing studies command line: {}", subCommandString);
        switch (subCommandString) {
            case "create":
                queryResponse = create();
                break;
            case "info":
                queryResponse = info();
                break;
            case "update":
                queryResponse = update();
                break;
            case "delete":
                queryResponse = delete();
                break;
            case "summary":
                queryResponse = summary();
                break;
            case "help":
                queryResponse = help();
                break;
            case "search":
                queryResponse = search();
                break;
            case "scan-files":
                queryResponse = scanFiles();
                break;
            case "resync-files":
                queryResponse = resyncFiles();
                break;
            case "files":
                queryResponse = files();
                break;
            case "samples":
                queryResponse = samples();
                break;
            case "jobs":
                queryResponse = jobs();
                break;
            case "variants":
                queryResponse = variants();
                break;
            case "acl":
                queryResponse = aclCommandExecutor.acls(studiesCommandOptions.aclsCommandOptions, openCGAClient.getStudyClient());
                break;
            case "acl-create":
                queryResponse = aclCommandExecutor.aclsCreateTemplate(studiesCommandOptions.aclsCreateCommandOptions,
                        openCGAClient.getStudyClient());
                break;
            case "acl-member-delete":
                queryResponse = aclCommandExecutor.aclMemberDelete(studiesCommandOptions.aclsMemberDeleteCommandOptions,
                        openCGAClient.getStudyClient());
                break;
            case "acl-member-info":
                queryResponse = aclCommandExecutor.aclMemberInfo(studiesCommandOptions.aclsMemberInfoCommandOptions,
                        openCGAClient.getStudyClient());
                break;
            case "acl-member-update":
                queryResponse = aclCommandExecutor.aclMemberUpdate(studiesCommandOptions.aclsMemberUpdateCommandOptions,
                        openCGAClient.getStudyClient());
                break;
            case "groups":
                queryResponse = groups();
                break;
            case "groups-create":
                queryResponse = groupsCreate();
                break;
            case "groups-delete":
                queryResponse = groupsDelete();
                break;
            case "groups-info":
                queryResponse = groupsInfo();
                break;
            case "groups-update":
                queryResponse = groupsUpdate();
                break;
            default:
                logger.error("Subcommand not valid");
                break;
        }

        createOutput(queryResponse);
    }

    /**********************************************  Administration Commands  ***********************************************/

    private QueryResponse<Study> create() throws CatalogException, IOException {
        logger.debug("Creating a new study");

        String name = studiesCommandOptions.createCommandOptions.name;
        String alias = studiesCommandOptions.createCommandOptions.alias;
        String projectId = studiesCommandOptions.createCommandOptions.project;
        String type = studiesCommandOptions.createCommandOptions.type;

        ObjectMap params = new ObjectMap();
        params.putIfNotNull(StudyDBAdaptor.QueryParams.DESCRIPTION.key(), studiesCommandOptions.createCommandOptions.description);
        if (type != null) {
            try {
                params.append(StudyDBAdaptor.QueryParams.TYPE.key(), Study.Type.valueOf(type));
            } catch (IllegalArgumentException e) {
                logger.error("{} not recognized as a proper study type", type);
                return null;
            }
        }

        return openCGAClient.getStudyClient().create(projectId, name, alias, params);
    }

    private QueryResponse<Study> info() throws CatalogException, IOException {
        logger.debug("Getting the study info");
        QueryOptions queryOptions = new QueryOptions();
        queryOptions.putIfNotEmpty(QueryOptions.INCLUDE, studiesCommandOptions.infoCommandOptions.dataModelOptions.include);
        queryOptions.putIfNotEmpty(QueryOptions.EXCLUDE, studiesCommandOptions.infoCommandOptions.dataModelOptions.exclude);
        return openCGAClient.getStudyClient().get(studiesCommandOptions.infoCommandOptions.study, queryOptions);
    }

    private QueryResponse<Study> update() throws CatalogException, IOException {
        logger.debug("Updating the study");

        ObjectMap params = new ObjectMap();
        params.putIfNotNull(StudyDBAdaptor.QueryParams.NAME.key(), studiesCommandOptions.updateCommandOptions.name);
        params.putIfNotNull(StudyDBAdaptor.QueryParams.TYPE.key(), studiesCommandOptions.updateCommandOptions.type);
        params.putIfNotNull(StudyDBAdaptor.QueryParams.DESCRIPTION.key(), studiesCommandOptions.updateCommandOptions.description);
        params.putIfNotNull(StudyDBAdaptor.QueryParams.STATS.key(), studiesCommandOptions.updateCommandOptions.stats);
        params.putIfNotNull(StudyDBAdaptor.QueryParams.ATTRIBUTES.key(), studiesCommandOptions.updateCommandOptions.attributes);
        return openCGAClient.getStudyClient().update(studiesCommandOptions.updateCommandOptions.study, params);
    }

    private QueryResponse<Study> delete() throws CatalogException, IOException {
        logger.debug("Deleting a study");
        ObjectMap objectMap = new ObjectMap();
        return openCGAClient.getStudyClient().delete(studiesCommandOptions.deleteCommandOptions.study, objectMap);
    }

    /************************************************  Summary and help Commands  ***********************************************/

    private QueryResponse<StudySummary> summary() throws CatalogException, IOException {
        logger.debug("Doing summary with the general stats of a study");
        QueryOptions queryOptions = new QueryOptions();
        return openCGAClient.getStudyClient().getSummary(studiesCommandOptions.summaryCommandOptions.study, queryOptions);
    }

    private QueryResponse<Study> help() throws CatalogException, IOException {
        logger.debug("Helping");
        /*QueryOptions queryOptions = new QueryOptions();
        QueryResponse<Study> study =
                openCGAClient.getStudyClient().help(queryOptions);
        System.out.println("Help: " + study);*/
        System.out.println("PENDING");
        return null;
    }

    /************************************************  Search Commands  ***********************************************/

    private QueryResponse<Study> search() throws CatalogException, IOException {
        logger.debug("Searching study");

        Query query = new Query();
        QueryOptions queryOptions = new QueryOptions();

        query.putIfNotEmpty(StudyDBAdaptor.QueryParams.PROJECT_ID.key(), studiesCommandOptions.searchCommandOptions.project);
        query.putIfNotEmpty(StudyDBAdaptor.QueryParams.NAME.key(), studiesCommandOptions.searchCommandOptions.name);
        query.putIfNotEmpty(StudyDBAdaptor.QueryParams.ALIAS.key(), studiesCommandOptions.searchCommandOptions.alias);
        query.putIfNotEmpty(StudyDBAdaptor.QueryParams.CREATION_DATE.key(), studiesCommandOptions.searchCommandOptions.creationDate);
        query.putIfNotEmpty(StudyDBAdaptor.QueryParams.STATUS_NAME.key(), studiesCommandOptions.searchCommandOptions.status);
        query.putIfNotEmpty(StudyDBAdaptor.QueryParams.ATTRIBUTES.key(), studiesCommandOptions.searchCommandOptions.attributes);
        query.putIfNotEmpty(StudyDBAdaptor.QueryParams.NATTRIBUTES.key(), studiesCommandOptions.searchCommandOptions.nattributes);
        query.putIfNotEmpty(StudyDBAdaptor.QueryParams.BATTRIBUTES.key(), studiesCommandOptions.searchCommandOptions.battributes);

        String type = studiesCommandOptions.searchCommandOptions.type;
        if (StringUtils.isNotEmpty(type)) {
            try {
                query.append(StudyDBAdaptor.QueryParams.TYPE.key(), Study.Type.valueOf(type));
            } catch (IllegalArgumentException e) {
                logger.error("{} not recognized as a proper study type", type);
                return null;
            }
        }

        queryOptions.putIfNotEmpty(QueryOptions.INCLUDE, studiesCommandOptions.searchCommandOptions.dataModelOptions.include);
        queryOptions.putIfNotEmpty(QueryOptions.EXCLUDE, studiesCommandOptions.searchCommandOptions.dataModelOptions.exclude);
        queryOptions.putIfNotEmpty(QueryOptions.LIMIT, studiesCommandOptions.searchCommandOptions.numericOptions.limit);
        queryOptions.putIfNotEmpty(QueryOptions.SKIP, studiesCommandOptions.searchCommandOptions.numericOptions.skip);
        queryOptions.put("count", studiesCommandOptions.searchCommandOptions.numericOptions.count);

        return openCGAClient.getStudyClient().search(query, queryOptions);
    }

    private QueryResponse scanFiles() throws CatalogException, IOException {
        logger.debug("Scan the study folder to find changes.\n");
        return openCGAClient.getStudyClient().scanFiles(studiesCommandOptions.scanFilesCommandOptions.study, null);
    }

    private QueryResponse resyncFiles() throws CatalogException, IOException {
        logger.debug("Scan the study folder to find changes.\n");
        return openCGAClient.getStudyClient().resyncFiles(studiesCommandOptions.resyncFilesCommandOptions.study, null);
    }

    private QueryResponse<File> files() throws CatalogException, IOException {
        logger.debug("Listing files of a study [PENDING]");

        QueryOptions queryOptions = new QueryOptions();
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.ID.key(), studiesCommandOptions.filesCommandOptions.file);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.NAME.key(), studiesCommandOptions.filesCommandOptions.name);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.PATH.key(), studiesCommandOptions.filesCommandOptions.path);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.TYPE.key(), studiesCommandOptions.filesCommandOptions.type);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.BIOFORMAT.key(), studiesCommandOptions.filesCommandOptions.bioformat);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.FORMAT.key(), studiesCommandOptions.filesCommandOptions.format);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.STATUS.key(), studiesCommandOptions.filesCommandOptions.status);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.DIRECTORY.key(), studiesCommandOptions.filesCommandOptions.directory);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.OWNER_ID.key(), studiesCommandOptions.filesCommandOptions.ownerId);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.CREATION_DATE.key(), studiesCommandOptions.filesCommandOptions.creationDate);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.MODIFICATION_DATE.key(),
                studiesCommandOptions.filesCommandOptions.modificationDate);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.DESCRIPTION.key(), studiesCommandOptions.filesCommandOptions.description);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.DISK_USAGE.key(), studiesCommandOptions.filesCommandOptions.diskUsage);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.SAMPLE_IDS.key(), studiesCommandOptions.filesCommandOptions.sampleIds);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.JOB_ID.key(), studiesCommandOptions.filesCommandOptions.jobId);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.ATTRIBUTES.key(), studiesCommandOptions.filesCommandOptions.attributes);
        queryOptions.putIfNotEmpty(FileDBAdaptor.QueryParams.NATTRIBUTES.key(), studiesCommandOptions.filesCommandOptions.nattributes);
        queryOptions.putIfNotEmpty(QueryOptions.INCLUDE, studiesCommandOptions.filesCommandOptions.dataModelOptions.include);
        queryOptions.putIfNotEmpty(QueryOptions.EXCLUDE, studiesCommandOptions.filesCommandOptions.dataModelOptions.exclude);
        queryOptions.putIfNotEmpty(QueryOptions.LIMIT, studiesCommandOptions.filesCommandOptions.numericOptions.limit);
        queryOptions.putIfNotEmpty(QueryOptions.SKIP, studiesCommandOptions.filesCommandOptions.numericOptions.skip);
        queryOptions.put("count", studiesCommandOptions.filesCommandOptions.numericOptions.count);

        return openCGAClient.getStudyClient().getFiles(studiesCommandOptions.filesCommandOptions.study, queryOptions);
    }

    private QueryResponse<Job> jobs() throws CatalogException, IOException {
        logger.debug("Listing jobs of a study. [PENDING]");

        QueryOptions queryOptions = new QueryOptions();
        if (StringUtils.isNotEmpty(studiesCommandOptions.jobsCommandOptions.name)) {
            queryOptions.put(JobDBAdaptor.QueryParams.NAME.key(), studiesCommandOptions.jobsCommandOptions.name);
        }

        if (StringUtils.isNotEmpty(studiesCommandOptions.jobsCommandOptions.toolName)) {
            queryOptions.put(JobDBAdaptor.QueryParams.TOOL_NAME.key(), studiesCommandOptions.jobsCommandOptions.toolName);
        }

        if (StringUtils.isNotEmpty(studiesCommandOptions.jobsCommandOptions.status)) {
            queryOptions.put(JobDBAdaptor.QueryParams.STATUS_NAME.key(), studiesCommandOptions.jobsCommandOptions.status);
        }
        if (StringUtils.isNotEmpty(studiesCommandOptions.jobsCommandOptions.ownerId)) {
            queryOptions.put(JobDBAdaptor.QueryParams.USER_ID.key(), studiesCommandOptions.jobsCommandOptions.ownerId);
        }
        /*if (StringUtils.isNotEmpty(studiesCommandOptions.jobsCommandOptions.date)) {
            queryOptions.put(CatalogJobDBAdaptor.QueryParams.CREATION_DATE.key(), studiesCommandOptions.jobsCommandOptions.date);
        }*/

        if (StringUtils.isNotEmpty(studiesCommandOptions.jobsCommandOptions.inputFiles)) {
            queryOptions.put(JobDBAdaptor.QueryParams.INPUT.key(), studiesCommandOptions.jobsCommandOptions.inputFiles);
        }
        if (StringUtils.isNotEmpty(studiesCommandOptions.jobsCommandOptions.outputFiles)) {
            queryOptions.put(JobDBAdaptor.QueryParams.OUTPUT.key(), studiesCommandOptions.jobsCommandOptions.outputFiles);
        }

        if (StringUtils.isNotEmpty(studiesCommandOptions.jobsCommandOptions.dataModelOptions.include)) {
            queryOptions.put(QueryOptions.INCLUDE, studiesCommandOptions.jobsCommandOptions.dataModelOptions.include);
        }
        if (StringUtils.isNotEmpty(studiesCommandOptions.jobsCommandOptions.dataModelOptions.exclude)) {
            queryOptions.put(QueryOptions.EXCLUDE, studiesCommandOptions.jobsCommandOptions.dataModelOptions.exclude);
        }

        if (StringUtils.isNotEmpty(studiesCommandOptions.jobsCommandOptions.numericOptions.limit)) {
            queryOptions.put(QueryOptions.LIMIT, studiesCommandOptions.jobsCommandOptions.numericOptions.limit);
        }
        if (StringUtils.isNotEmpty(studiesCommandOptions.jobsCommandOptions.numericOptions.skip)) {
            queryOptions.put(QueryOptions.SKIP, studiesCommandOptions.jobsCommandOptions.numericOptions.skip);
        }

        queryOptions.put("count", studiesCommandOptions.jobsCommandOptions.numericOptions.count);

        return openCGAClient.getStudyClient().getJobs(studiesCommandOptions.jobsCommandOptions.study, queryOptions);
    }


    private QueryResponse<Sample> samples() throws CatalogException, IOException {

        logger.debug("Listing samples of a study. [PENDING]");

        QueryOptions queryOptions = new QueryOptions();
        if (StringUtils.isNotEmpty(studiesCommandOptions.samplesCommandOptions.name)) {
            queryOptions.put(SampleDBAdaptor.QueryParams.NAME.key(), studiesCommandOptions.samplesCommandOptions.name);
        }
        if (StringUtils.isNotEmpty(studiesCommandOptions.samplesCommandOptions.source)) {
            queryOptions.put(SampleDBAdaptor.QueryParams.SOURCE.key(), studiesCommandOptions.samplesCommandOptions.source);
        }
        if (StringUtils.isNotEmpty(studiesCommandOptions.samplesCommandOptions.individual)) {
            queryOptions.put(SampleDBAdaptor.QueryParams.INDIVIDUAL_ID.key(),
                    studiesCommandOptions.samplesCommandOptions.individual);
        }
        if (StringUtils.isNotEmpty(studiesCommandOptions.samplesCommandOptions.annotationSetName)) {
            queryOptions.put(SampleDBAdaptor.QueryParams.ANNOTATION_SET_NAME.key(),
                    studiesCommandOptions.samplesCommandOptions.annotationSetName);
        }
        if (StringUtils.isNotEmpty(studiesCommandOptions.samplesCommandOptions.variableSetId)) {
            queryOptions.put(SampleDBAdaptor.QueryParams.VARIABLE_SET_ID.key(),
                    studiesCommandOptions.samplesCommandOptions.variableSetId);
        }
        if (StringUtils.isNotEmpty(studiesCommandOptions.samplesCommandOptions.annotation)) {
            queryOptions.put(SampleDBAdaptor.QueryParams.ANNOTATION.key(), studiesCommandOptions.samplesCommandOptions.annotation);
        }
        /*if (StringUtils.isNotEmpty(studiesCommandOptions.samplesCommandOptions.description)) {
            queryOptions.put(CatalogSampleDBAdaptor.QueryParams.DESCRIPTION.key(), studiesCommandOptions.samplesCommandOptions.description);
        }*/
        if (StringUtils.isNotEmpty(studiesCommandOptions.samplesCommandOptions.dataModelOptions.include)) {
            queryOptions.put(QueryOptions.INCLUDE, studiesCommandOptions.samplesCommandOptions.dataModelOptions.include);
        }
        if (StringUtils.isNotEmpty(studiesCommandOptions.samplesCommandOptions.dataModelOptions.exclude)) {
            queryOptions.put(QueryOptions.EXCLUDE, studiesCommandOptions.samplesCommandOptions.dataModelOptions.exclude);
        }
        if (StringUtils.isNotEmpty(studiesCommandOptions.samplesCommandOptions.numericOptions.limit)) {
            queryOptions.put(QueryOptions.LIMIT, studiesCommandOptions.samplesCommandOptions.numericOptions.limit);
        }
        if (StringUtils.isNotEmpty(studiesCommandOptions.samplesCommandOptions.numericOptions.skip)) {
            queryOptions.put(QueryOptions.SKIP, studiesCommandOptions.samplesCommandOptions.numericOptions.skip);
        }
        queryOptions.put("count", studiesCommandOptions.samplesCommandOptions.numericOptions.count);

        return openCGAClient.getStudyClient().getSamples(studiesCommandOptions.samplesCommandOptions.study, queryOptions);
    }

    @Deprecated
    private QueryResponse variants() throws CatalogException, IOException {

        logger.debug("Listing variants of a study.");

        QueryOptions queryOptions = new QueryOptions();
        queryOptions.putAll(studiesCommandOptions.commonCommandOptions.params);

        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ID.key(), studiesCommandOptions.variantsCommandOptions.ids);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.REGION.key(), studiesCommandOptions.variantsCommandOptions.region);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.CHROMOSOME.key(),
                studiesCommandOptions.variantsCommandOptions.chromosome);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.GENE.key(), studiesCommandOptions.variantsCommandOptions.gene);
        queryOptions.putIfNotNull(CatalogVariantDBAdaptor.VariantQueryParams.TYPE.key(), studiesCommandOptions.variantsCommandOptions.type);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.REFERENCE.key(),
                studiesCommandOptions.variantsCommandOptions.reference);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ALTERNATE.key(),
                studiesCommandOptions.variantsCommandOptions.alternate);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.RETURNED_STUDIES.key(),
                studiesCommandOptions.variantsCommandOptions.returnedStudies);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.RETURNED_SAMPLES.key(),
                studiesCommandOptions.variantsCommandOptions.returnedSamples);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.RETURNED_FILES.key(),
                studiesCommandOptions.variantsCommandOptions.returnedFiles);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.FILES.key(), studiesCommandOptions.variantsCommandOptions.files);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.STATS_MAF.key(), studiesCommandOptions.variantsCommandOptions.maf);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.STATS_MGF.key(), studiesCommandOptions.variantsCommandOptions.mgf);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.MISSING_ALLELES.key(),
                studiesCommandOptions.variantsCommandOptions.missingAlleles);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.MISSING_GENOTYPES.key(),
                studiesCommandOptions.variantsCommandOptions.missingGenotypes);
//        queryOptions.put(CatalogVariantDBAdaptor.VariantQueryParams.ANNOTATION_EXISTS.key(),
//                studiesCommandOptions.variantsCommandOptions.annotationExists);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.GENOTYPE.key(),
                studiesCommandOptions.variantsCommandOptions.genotype);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_CONSEQUENCE_TYPE.key(),
                studiesCommandOptions.variantsCommandOptions.annot_ct);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_XREF.key(),
                studiesCommandOptions.variantsCommandOptions.annot_xref);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_BIOTYPE.key(),
                studiesCommandOptions.variantsCommandOptions.annot_biotype);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_POLYPHEN.key(),
                studiesCommandOptions.variantsCommandOptions.polyphen);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_SIFT.key(), studiesCommandOptions.variantsCommandOptions.sift);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_CONSERVATION.key(),
                studiesCommandOptions.variantsCommandOptions.conservation);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_POPULATION_MINOR_ALLELE_FREQUENCY.key(),
                studiesCommandOptions.variantsCommandOptions.annotPopulationMaf);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_POPULATION_ALTERNATE_FREQUENCY.key(),
                studiesCommandOptions.variantsCommandOptions.alternate_frequency);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_POPULATION_REFERENCE_FREQUENCY.key(),
                studiesCommandOptions.variantsCommandOptions.reference_frequency);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_TRANSCRIPTION_FLAGS.key(),
                studiesCommandOptions.variantsCommandOptions.transcriptionFlags);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_GENE_TRAITS_ID.key(),
                studiesCommandOptions.variantsCommandOptions.geneTraitId);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_GENE_TRAITS_NAME.key(),
                studiesCommandOptions.variantsCommandOptions.geneTraitName);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_HPO.key(),
                studiesCommandOptions.variantsCommandOptions.hpo);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_GO.key(),
                studiesCommandOptions.variantsCommandOptions.go);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_EXPRESSION.key(),
                studiesCommandOptions.variantsCommandOptions.expression);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_PROTEIN_KEYWORDS.key(),
                studiesCommandOptions.variantsCommandOptions.proteinKeyword);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_DRUG.key(),
                studiesCommandOptions.variantsCommandOptions.drug);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.ANNOT_FUNCTIONAL_SCORE.key(),
                studiesCommandOptions.variantsCommandOptions.functionalScore);
        queryOptions.putIfNotEmpty(CatalogVariantDBAdaptor.VariantQueryParams.UNKNOWN_GENOTYPE.key(),
                studiesCommandOptions.variantsCommandOptions.unknownGenotype);
        queryOptions.put(QueryOptions.SORT, studiesCommandOptions.variantsCommandOptions.sort);
//        queryOptions.putIfNotEmpty("merge", studiesCommandOptions.variantsCommandOptions.merge);

        queryOptions.putIfNotEmpty(QueryOptions.INCLUDE, studiesCommandOptions.variantsCommandOptions.include);
        queryOptions.putIfNotEmpty(QueryOptions.EXCLUDE, studiesCommandOptions.variantsCommandOptions.exclude);
        queryOptions.putIfNotEmpty(QueryOptions.LIMIT, studiesCommandOptions.variantsCommandOptions.limit);
        queryOptions.putIfNotEmpty(QueryOptions.SKIP, studiesCommandOptions.variantsCommandOptions.skip);

        queryOptions.put("samplesMetadata", studiesCommandOptions.variantsCommandOptions.samplesMetadata);
        queryOptions.putIfNotEmpty("groupBy", studiesCommandOptions.variantsCommandOptions.groupBy);
        queryOptions.put("histogram", studiesCommandOptions.variantsCommandOptions.histogram);
        queryOptions.putIfNotEmpty("interval", studiesCommandOptions.variantsCommandOptions.interval);
        queryOptions.put("count", studiesCommandOptions.variantsCommandOptions.count);

        if (studiesCommandOptions.variantsCommandOptions.count) {
            return openCGAClient.getStudyClient().countVariants(studiesCommandOptions.variantsCommandOptions.study, queryOptions);
        } else if (studiesCommandOptions.variantsCommandOptions.samplesMetadata || StringUtils.isNoneEmpty(studiesCommandOptions.variantsCommandOptions.groupBy) || studiesCommandOptions.variantsCommandOptions.histogram) {
            return openCGAClient.getStudyClient().getVariantsGeneric(studiesCommandOptions.variantsCommandOptions.study, queryOptions);
        } else {
            return openCGAClient.getStudyClient().getVariants(studiesCommandOptions.variantsCommandOptions.study, queryOptions);
        }
    }

    /************************************************* Groups commands *********************************************************/
    private QueryResponse<ObjectMap> groups() throws CatalogException,IOException {
        logger.debug("Groups");
        QueryOptions queryOptions = new QueryOptions();
        return openCGAClient.getStudyClient().groups(studiesCommandOptions.groupsCommandOptions.study, queryOptions);
    }

    private QueryResponse<ObjectMap> groupsCreate() throws CatalogException,IOException {
        logger.debug("Creating groups");
        QueryOptions queryOptions = new QueryOptions();
        return openCGAClient.getStudyClient().createGroup(studiesCommandOptions.groupsCreateCommandOptions.study,
                studiesCommandOptions.groupsCreateCommandOptions.groupId, studiesCommandOptions.groupsCreateCommandOptions.users,
                queryOptions);
    }

    private QueryResponse<ObjectMap> groupsDelete() throws CatalogException,IOException {

        logger.debug("Deleting groups");
        QueryOptions queryOptions = new QueryOptions();
        return openCGAClient.getStudyClient().deleteGroup(studiesCommandOptions.groupsDeleteCommandOptions.study,
                studiesCommandOptions.groupsDeleteCommandOptions.groupId, queryOptions);
    }

    private QueryResponse<ObjectMap> groupsInfo() throws CatalogException,IOException {

        logger.debug("Info groups");
        QueryOptions queryOptions = new QueryOptions();
        return openCGAClient.getStudyClient().infoGroup(studiesCommandOptions.groupsInfoCommandOptions.study,
                studiesCommandOptions.groupsInfoCommandOptions.groupId, queryOptions);
    }
    private QueryResponse<ObjectMap> groupsUpdate() throws CatalogException,IOException {

        logger.debug("Updating groups");

        QueryOptions queryOptions = new QueryOptions();

        queryOptions.putIfNotEmpty(StudyClient.GroupUpdateParams.ADD_USERS.key(),
                studiesCommandOptions.groupsUpdateCommandOptions.addUsers);
        queryOptions.putIfNotEmpty(StudyClient.GroupUpdateParams.SET_USERS.key(),
                studiesCommandOptions.groupsUpdateCommandOptions.setUsers);
        queryOptions.putIfNotEmpty(StudyClient.GroupUpdateParams.REMOVE_USERS.key(),
                studiesCommandOptions.groupsUpdateCommandOptions.removeUsers);

        return openCGAClient.getStudyClient().updateGroup(studiesCommandOptions.groupsUpdateCommandOptions.study,
                studiesCommandOptions.groupsUpdateCommandOptions.groupId, queryOptions);
    }

}
