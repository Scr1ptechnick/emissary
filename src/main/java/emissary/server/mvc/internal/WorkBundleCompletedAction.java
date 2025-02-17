package emissary.server.mvc.internal;

import emissary.core.Namespace;
import emissary.core.NamespaceException;
import emissary.pickup.WorkSpace;
import emissary.server.mvc.adapters.RequestUtil;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.Consumes;
import javax.ws.rs.FormParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import static emissary.server.mvc.adapters.WorkSpaceAdapter.CLIENT_NAME;
import static emissary.server.mvc.adapters.WorkSpaceAdapter.SPACE_NAME;
import static emissary.server.mvc.adapters.WorkSpaceAdapter.WORK_BUNDLE_ID;
import static emissary.server.mvc.adapters.WorkSpaceAdapter.WORK_BUNDLE_STATUS;

/**
 * Web-tier worker to call into the TreeSpace with notification that a work bundle was completed
 */
@Path("")
// context is /emissary, set in EmissaryServer
public class WorkBundleCompletedAction {
    private final Logger logger = LoggerFactory.getLogger(this.getClass());

    /*
     * <!-- Notify a WorkSpace that work was completed --> <Use-Case source="*" action="/WorkBundleCompleted.action"> <Work
     * type="Bean" target="emissary.comms.http.worker.LogWorker"/> <Work type="Bean"
     * target="emissary.comms.http.worker.BundleCompletedWorker"/> <View status="0" view="/success.jsp"/> <View status="-1"
     * view="/error.jsp"/> </Use-Case>
     */

    @POST
    @Path("/WorkBundleCompleted.action")
    @Consumes(MediaType.APPLICATION_FORM_URLENCODED)
    @Produces(MediaType.TEXT_PLAIN)
    public Response postWorkBundleCompleted(@FormParam(CLIENT_NAME) String placeName, @FormParam(SPACE_NAME) String spaceName,
            @FormParam(WORK_BUNDLE_ID) String bundleId, @FormParam(WORK_BUNDLE_STATUS) boolean itWorked) {
        String cleanPlaceName = RequestUtil.sanitizeParameter(placeName);
        String cleanSpaceName = RequestUtil.sanitizeParameter(spaceName);
        String cleanBundleId = RequestUtil.sanitizeParameter(bundleId);
        if (StringUtils.isBlank(cleanPlaceName) || StringUtils.isBlank(cleanSpaceName) || StringUtils.isBlank(cleanBundleId)
                || !cleanSpaceName.startsWith("WORKSPACE.WORK_SPACE.INPUT.") || !cleanPlaceName.startsWith("INITIAL.FILE_PICK_UP_CLIENT.INPUT.")) {
            return Response
                    .serverError()
                    .entity("Bad params: " + CLIENT_NAME + " - " + cleanPlaceName + ", " + SPACE_NAME + " - " + cleanSpaceName + ", " + WORK_BUNDLE_ID
                            + " - " + cleanBundleId)
                    .build();
        }

        try {
            return workBundleCompleted(cleanSpaceName, cleanPlaceName, cleanBundleId, itWorked);
        } catch (NamespaceException e) {
            logger.error("There was a problem while processing the WorkBundle", e);
            return Response.serverError().entity("There was a problem while processing the WorkBundle: " + e.getMessage()).build();
        }
    }

    private Response workBundleCompleted(String spaceName, String placeName, String bundleId, boolean itWorked) throws NamespaceException,
            IllegalArgumentException {
        // Look up the place reference
        // TODO Figure out why we have to remove the key prefix now
        final String workspaceNamespaceKey = spaceName.substring("WORKSPACE.WORK_SPACE.INPUT.".length());
        final WorkSpace space = (WorkSpace) Namespace.lookup(workspaceNamespaceKey);

        // TODO Figure out why we have to remove the key prefix now
        String pickupClientNamespaceKey = placeName.substring("INITIAL.FILE_PICK_UP_CLIENT.INPUT.".length());
        if (space.workCompleted(pickupClientNamespaceKey, bundleId, itWorked)) {
            // old success from BundleCompletedWorker
            // return WORKER_SUCCESS;
            return Response.ok().entity("Work Bundle Completed").build();
        } else {
            // old failure from BundleCompletedWorker
            // return new WorkerStatus(WorkerStatus.FAILURE, "BundleCompletedWorker exception", e);
            return Response.serverError().entity("Work Bundle not completed").build();
        }
    }
}
