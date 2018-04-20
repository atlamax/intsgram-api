package com.mobcrush.instagram.service;

import com.mobcrush.instagram.request.CreateLiveRequest;
import com.mobcrush.instagram.request.CreateLiveResult;
import com.mobcrush.instagram.request.EndLiveRequest;
import com.mobcrush.instagram.request.StartLiveRequest;
import com.mobcrush.instagram.request.payload.CreateLivePayload;
import com.mobcrush.instagram.request.payload.EndLivePayload;
import com.mobcrush.instagram.request.payload.StartLivePayload;
import org.apache.http.client.utils.URIBuilder;
import org.brunocvcunha.instagram4j.Instagram4j;
import org.brunocvcunha.instagram4j.requests.InstagramRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.net.URI;
import java.net.URISyntaxException;

import static org.apache.http.util.Asserts.notNull;

public class LiveBroadcastService {

    private static final Logger LOGGER = LoggerFactory.getLogger(LiveBroadcastService.class);

    private Instagram4j instagram;

    private static final String PREVIEW_WIDTH = "720";
    private static final String PREVIEW_HEIGHT = "1184";
    private static final String RTMP_SCHEME = "RTMP";
    private static final int RTMP_PORT = 80;

    /**
     * Default constructor
     *
     * @param instagram instagram
     */
    public LiveBroadcastService(Instagram4j instagram) {
        this.instagram = instagram;
    }

    /**
     * Start live broadcasting
     *
     * @return URI for broadcasting
     */
    @Nullable
    public URI start() {
        String csrfToken;
        try {
            csrfToken = instagram.getOrFetchCsrf();
        } catch (Exception e) {
            LOGGER.error("Error occurred during request for CSRF token", e);
            return null;
        }

        CreateLiveRequest createRequest = buildCreateRequest(instagram.getUuid(), csrfToken);
        CreateLiveResult createResponse = sendRequest(createRequest);
        if (createResponse == null) {
            return null;
        }

        StartLiveRequest startRequest = buildStartRequest(
                instagram.getUuid(), csrfToken, createResponse.getBroadcastId()
        );
        sendRequest(startRequest);

        return updateBroadcastingURL(createResponse.getUploadUrl());
    }

    /**
     * End live broadcasting
     *
     * @param broadcastId broadcast Id
     */
    public void end(@Nonnull String broadcastId) {
        notNull(instagram, "Instagram object must not be null");

        String csrfToken;
        try {
            csrfToken = instagram.getOrFetchCsrf();
        } catch (Exception e) {
            LOGGER.error("Error occurred during request for CSRF token", e);
            return;
        }

        EndLiveRequest request = buildEndRequest(csrfToken, broadcastId);
        sendRequest(request);
    }

    /**
     * Build request to create broadcast
     *
     * @param uuid      UUID
     * @param csrfToken CSRF token
     *
     * @return request
     */
    private CreateLiveRequest buildCreateRequest(String uuid, String csrfToken) {
        CreateLivePayload payload = new CreateLivePayload();
        payload.set_uuid(uuid);
        payload.set_csrftoken(csrfToken);
        payload.setPreview_height(PREVIEW_HEIGHT);
        payload.setPreview_width(PREVIEW_WIDTH);
        payload.setBroadcast_message("");
        payload.setBroadcast_type(RTMP_SCHEME);
        payload.setInternal_only("0");

        return new CreateLiveRequest(payload);
    }

    /**
     * Build request to start broadcasting
     *
     * @param uuid        UUID
     * @param csrfToken   CSRF token
     * @param broadcastId broadcast Id
     *
     * @return request
     */
    private StartLiveRequest buildStartRequest(String uuid, String csrfToken, String broadcastId) {
        StartLivePayload startLivePayload = new StartLivePayload();
        startLivePayload.set_uuid(uuid);
        startLivePayload.set_csrftoken(csrfToken);
        startLivePayload.setShould_send_notifications("1");

        return new StartLiveRequest(startLivePayload, broadcastId);
    }

    /**
     * Build request to end broadcasting
     *
     * @param csrfToken   CSRF token
     * @param broadcastId broadcast Id
     *
     * @return request
     */
    private EndLiveRequest buildEndRequest(String csrfToken, String broadcastId) {
        EndLivePayload payload = new EndLivePayload();
        payload.set_uid(String.valueOf(instagram.getUserId()));
        payload.set_uuid(instagram.getUuid());
        payload.set_csrftoken(csrfToken);

        return new EndLiveRequest(payload, broadcastId);
    }

    /**
     * Send request to Instagram
     *
     * @param request request
     * @param <T>     type of response
     *
     * @return response
     */
    private <T> T sendRequest(InstagramRequest<T> request) {
        try {
            return instagram.sendRequest(request);
        } catch (Exception e) {
            LOGGER.error("Error occurred during performing request", e);
            return null;
        }
    }

    /**
     * Update URL for broadcasting
     *
     * @param url broadcasting URL
     *
     * @return URL for broadcasting
     */
    private URI updateBroadcastingURL(String url) {
        try {
            return new URIBuilder(url)
                    .setScheme(RTMP_SCHEME)
                    .setPort(RTMP_PORT)
                    .build();
        } catch (URISyntaxException e) {
            LOGGER.error("Error occurred while updating RTMP URL for live streaming");
            return null;
        }
    }
}
