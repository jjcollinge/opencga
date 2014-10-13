package org.opencb.opencga.server;

import com.wordnik.swagger.annotations.Api;
import com.wordnik.swagger.annotations.ApiOperation;
import com.wordnik.swagger.annotations.ApiParam;
import org.glassfish.jersey.media.multipart.FormDataContentDisposition;
import org.glassfish.jersey.media.multipart.FormDataParam;
import org.opencb.biodata.models.feature.Region;
import org.opencb.datastore.core.ObjectMap;
import org.opencb.datastore.core.QueryOptions;
import org.opencb.datastore.core.QueryResult;
import org.opencb.opencga.catalog.beans.File;
import org.opencb.opencga.catalog.db.CatalogManagerException;
import org.opencb.opencga.catalog.io.CatalogIOManagerException;
import org.opencb.opencga.lib.common.IOUtils;
import org.opencb.opencga.storage.core.alignment.AlignmentStorageManager;
import org.opencb.opencga.storage.core.alignment.adaptors.AlignmentQueryBuilder;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.*;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;
import java.io.IOException;
import java.net.URI;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

@Path("/files")
@Api(value = "files", description = "files")
public class FileWSServer extends OpenCGAWSServer {




    private static AlignmentStorageManager alignmentStorageManager = null;
//    private static AlignmentQueryBuilder dbAdaptor = null;
    private static final String MONGODB_VARIANT_MANAGER = "org.opencb.opencga.storage.mongodb.variant.MongoDBVariantStorageManager";
    private static final String MONGODB_ALIGNMENT_MANAGER = "org.opencb.opencga.storage.mongodb.alignment.MongoDBAlignmentStorageManager";



    public FileWSServer(@PathParam("version") String version, @Context UriInfo uriInfo, @Context HttpServletRequest httpServletRequest)
            throws IOException, ClassNotFoundException, IllegalAccessException, InstantiationException {
        super(version, uriInfo, httpServletRequest);
//        String alignmentManagerName = properties.getProperty("STORAGE.ALIGNMENT-MANAGER", MONGODB_ALIGNMENT_MANAGER);
        String alignmentManagerName = MONGODB_ALIGNMENT_MANAGER;
        if(alignmentStorageManager == null) {
//            try {
                alignmentStorageManager = (AlignmentStorageManager) Class.forName(alignmentManagerName).newInstance();
//            } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
//                e.printStackTrace();
//                logger.error(e.getMessage(), e);
//            }
            //dbAdaptor = alignmentStorageManager.getDBAdaptor(null);
        }
    }

    @POST
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @Path("/upload")
    @Produces("application/json")
    public Response chunkUpload(@FormDataParam("chunk_content") byte[] chunkBytes,
                                @FormDataParam("chunk_content") FormDataContentDisposition contentDisposition,
                                @DefaultValue("") @FormDataParam("chunk_id") String chunk_id,
                                @DefaultValue("") @FormDataParam("last_chunk") String last_chunk,
                                @DefaultValue("") @FormDataParam("chunk_total") String chunk_total,
                                @DefaultValue("") @FormDataParam("chunk_size") String chunk_size,
                                @DefaultValue("") @FormDataParam("chunk_hash") String chunkHash,
                                @DefaultValue("false") @FormDataParam("resume_upload") String resume_upload,


                                @ApiParam(value = "filename", required = true) @DefaultValue("") @FormDataParam("filename") String filename,
                                @ApiParam(value = "fileFormat", required = true) @DefaultValue("") @FormDataParam("fileFormat") String fileFormat,
                                @ApiParam(value = "bioFormat", required = true) @DefaultValue("") @FormDataParam("bioFormat") String bioFormat,
                                @ApiParam(value = "userId", required = true) @DefaultValue("") @FormDataParam("userId") String userId,
                                @ApiParam(value = "projectId", required = true) @DefaultValue("") @FormDataParam("projectId") String projectId,
                                @ApiParam(value = "studyId", required = true) @FormDataParam("studyId") int studyId,
                                @ApiParam(value = "relativeFilePath", required = true) @DefaultValue("") @FormDataParam("relativeFilePath") String relativeFilePath,
                                @ApiParam(value = "description", required = true) @DefaultValue("") @FormDataParam("description") String description,
                                @ApiParam(value = "parents", required = true) @DefaultValue("true") @FormDataParam("parents") boolean parents) {

        long t = System.currentTimeMillis();

        java.nio.file.Path filePath = null;
        try {
            filePath = Paths.get(catalogManager.getFileUri(userId, projectId, String.valueOf(studyId), relativeFilePath));
            System.out.println(filePath);
        } catch (CatalogIOManagerException e) {
            System.out.println("catalogManager.getFilePath");
            e.printStackTrace();
        }

        java.nio.file.Path completedFilePath = filePath.getParent().resolve("_" + relativeFilePath);
        java.nio.file.Path folderPath = filePath.getParent().resolve("__" + relativeFilePath);


        logger.info(relativeFilePath + "");
        logger.info(folderPath + "");
        logger.info(filePath + "");
        boolean resume = Boolean.parseBoolean(resume_upload);

        try {
            logger.info("---resume is: " + resume);
            if (resume) {
                logger.info("Resume ms :" + (System.currentTimeMillis() - t));
                return createOkResponse(getResumeFileJSON(folderPath));
            }

            int chunkId = Integer.parseInt(chunk_id);
            int chunkSize = Integer.parseInt(chunk_size);
            boolean lastChunk = Boolean.parseBoolean(last_chunk);

            logger.info("---saving chunk: " + chunkId);
            logger.info("lastChunk: " + lastChunk);

            // WRITE CHUNK FILE
            if (!Files.exists(folderPath)) {
                logger.info("createDirectory(): " + folderPath);
                Files.createDirectory(folderPath);
            }
            logger.info("check dir " + Files.exists(folderPath));
            // String hash = StringUtils.sha1(new String(chunkBytes));
            // logger.info("bytesHash: " + hash);
            // logger.info("chunkHash: " + chunkHash);
            // hash = chunkHash;
            if (chunkBytes.length == chunkSize) {
                Files.write(folderPath.resolve(chunkId + "_" + chunkBytes.length + "_partial"), chunkBytes);
            }

            if (lastChunk) {
                logger.info("lastChunk is true...");
                Files.createFile(completedFilePath);
                List<java.nio.file.Path> chunks = getSortedChunkList(folderPath);
                logger.info("----ordered chunks length: " + chunks.size());
                for (java.nio.file.Path partPath : chunks) {
                    logger.info(partPath.getFileName().toString());
                    Files.write(completedFilePath, Files.readAllBytes(partPath), StandardOpenOption.APPEND);
                }
                IOUtils.deleteDirectory(folderPath);
                try {

                    QueryResult queryResult = catalogManager.uploadFile(studyId, fileFormat, bioFormat, relativeFilePath, description, parents, Files.newInputStream(completedFilePath), sessionId);
                    IOUtils.deleteDirectory(completedFilePath);

                    return createOkResponse(queryResult);
                } catch (Exception e) {
                    logger.error(e.toString());
                    return createErrorResponse(e.getMessage());
                }
            }

        } catch (IOException e) {

            // TODO Auto-generated catch block
            e.printStackTrace();
        }
        logger.info("chunk saved ms :" + (System.currentTimeMillis() - t));
        return createOkResponse("ok");
    }

    @GET
    @Path("/{fileId}/info")
    @Produces("application/json")
    @ApiOperation(value = "File info")
    public Response info(@PathParam(value = "fileId") @DefaultValue("") @FormDataParam("fileId") String fileId
    ) {
        try {
            QueryResult result = catalogManager.getFile(catalogManager.getFileId(fileId), sessionId);
            return createOkResponse(result);
        } catch (CatalogManagerException | CatalogIOManagerException | IOException e) {
            e.printStackTrace();
            return createErrorResponse(e.getMessage());
        }
    }

    @GET
    @Path("/{fileId}/list")
    @Produces("application/json")
    @ApiOperation(value = "List folder")
    public Response list(@PathParam(value = "fileId") @DefaultValue("") @FormDataParam("fileId") String fileId
    ) {
        try {
            int fileIdNum = catalogManager.getFileId(fileId);
            QueryResult result = catalogManager.getAllFilesInFolder(fileIdNum, sessionId);
            return createOkResponse(result);
        } catch (CatalogManagerException e) {
            e.printStackTrace();
            return createErrorResponse(e.getMessage());
        }
    }


    @GET
    @Path("/{fileId}/fetch")
    @Produces("application/json")
    @ApiOperation(value = "File fetch")
    public Response fetch(@PathParam(value = "fileId") @DefaultValue("") @FormDataParam("fileId") String fileId,
                          @ApiParam(value = "region", required = true) @DefaultValue("") @QueryParam("region") String region,
                          @ApiParam(value = "view_as_pairs", required = false) @DefaultValue("false") @QueryParam("view_as_pairs") boolean view_as_pairs,
                          @ApiParam(value = "include_coverage", required = false) @DefaultValue("true") @QueryParam("include_coverage") boolean include_coverage,
                          @ApiParam(value = "process_differences", required = false) @DefaultValue("true") @QueryParam("process_differences") boolean process_differences,
                          @ApiParam(value = "histogram", required = false) @DefaultValue("false") @QueryParam("histogram") boolean histogram,
                          @ApiParam(value = "interval", required = false) @DefaultValue("2000") @QueryParam("interval") int interval
    ) {
        int fileIdNum;
        File file;
        URI fileUri;
        String dbName = null;   //TODO: getDBName from fileStats?   dbName == userId
        Region r = new Region(region);

        try {
            System.out.println("catalogManager = " + catalogManager);
            fileIdNum = catalogManager.getFileId(fileId);
            QueryResult<File> queryResult = catalogManager.getFile(fileIdNum, sessionId);
            file = queryResult.getResult().get(0);
            fileUri = catalogManager.getFileUri(file);
        } catch (CatalogManagerException | CatalogIOManagerException | IOException e) {
            e.printStackTrace();
            return createErrorResponse(e.getMessage());
        }

        switch(file.getBioformat()) {
            case "bam":
                //TODO: Check indexed
                QueryOptions options = new QueryOptions();
                options.put(AlignmentQueryBuilder.QO_FILE_ID, Integer.toString(fileIdNum));
                options.put(AlignmentQueryBuilder.QO_BAM_PATH, fileUri.getPath());
                options.put(AlignmentQueryBuilder.QO_VIEW_AS_PAIRS, view_as_pairs);
                options.put(AlignmentQueryBuilder.QO_INCLUDE_COVERAGE, include_coverage);
                options.put(AlignmentQueryBuilder.QO_PROCESS_DIFFERENCES, process_differences);
                options.put(AlignmentQueryBuilder.QO_BATCH_SIZE, interval);
                options.put(AlignmentQueryBuilder.QO_HISTOGRAM, histogram);

                AlignmentQueryBuilder dbAdaptor = alignmentStorageManager.getDBAdaptor(dbName);
                QueryResult alignmentsByRegion;
                if(histogram){
                    alignmentsByRegion = dbAdaptor.getAllIntervalFrequencies(r, options);
                } else {
                    alignmentsByRegion = dbAdaptor.getAllAlignmentsByRegion(r, options);
                }
                return createOkResponse(alignmentsByRegion);
            default:
                return createErrorResponse("Unknown bioformat '" + file.getBioformat() + '\'');
        }
    }


    private ObjectMap getResumeFileJSON(java.nio.file.Path folderPath) throws IOException {
        ObjectMap objectMap = new ObjectMap();

        if (Files.exists(folderPath)) {
            DirectoryStream<java.nio.file.Path> folderStream = Files.newDirectoryStream(folderPath, "*_partial");
            for (java.nio.file.Path partPath : folderStream) {
                String[] nameSplit = partPath.getFileName().toString().split("_");
                ObjectMap chunkInfo = new ObjectMap();
                chunkInfo.put("size", Integer.parseInt(nameSplit[1]));
                objectMap.put(nameSplit[0], chunkInfo);
            }
        }

        return objectMap;
    }

    private List<java.nio.file.Path> getSortedChunkList(java.nio.file.Path folderPath) throws IOException {
        List<java.nio.file.Path> files = new ArrayList<>();
        DirectoryStream<java.nio.file.Path> stream = Files.newDirectoryStream(folderPath, "*_partial");
        for (java.nio.file.Path p : stream) {
            logger.info("adding to ArrayList: " + p.getFileName());
            files.add(p);
        }
        logger.info("----ordered files length: " + files.size());
        Collections.sort(files, new Comparator<java.nio.file.Path>() {
            public int compare(java.nio.file.Path o1, java.nio.file.Path o2) {
                int id_o1 = Integer.parseInt(o1.getFileName().toString().split("_")[0]);
                int id_o2 = Integer.parseInt(o2.getFileName().toString().split("_")[0]);
                logger.info(id_o1 + "");
                logger.info(id_o2 + "");
                return id_o1 - id_o2;
            }
        });
        return files;
    }
}
