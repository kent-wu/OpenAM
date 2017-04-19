package org.forgerock.openam.selfservice.stage;

import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.oauth2.core.*;
import org.forgerock.oauth2.core.exceptions.*;
import org.forgerock.openam.cts.exceptions.CoreTokenException;
import org.forgerock.openam.oauth2.OAuthTokenStore;
import org.forgerock.openam.oauth2.OpenAMAuthorizationCode;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.util.RequirementsBuilder;
import org.forgerock.util.Reject;

import javax.inject.Inject;
import java.util.*;

public class IssueCodeStage implements ProgressStage<IssueCodeConfig> {
    private final OAuthTokenStore tokenStore;
    private final OAuth2ProviderSettingsFactory providerSettingsFactory;

    @Inject
    public IssueCodeStage(OAuthTokenStore tokenStore, OAuth2ProviderSettingsFactory providerSettingsFactory) {
        this.tokenStore = tokenStore;
        this.providerSettingsFactory = providerSettingsFactory;
    }

    public JsonValue gatherInitialRequirements(ProcessContext context, IssueCodeConfig config) throws ResourceException {
        Reject.ifFalse(context.containsState("user"), "Issue Code stage expects user in the context");
        return RequirementsBuilder.newEmptyRequirements();
    }

    public StageResponse advance(ProcessContext context, IssueCodeConfig config) throws ResourceException {

        JsonValue userState = context.getState("user");
        Set<String> scopes = context.getState("scopes").asSet(String.class);
        String clientId = context.getState("clientId").asString();
        String redirectUri = context.getState("redirectUri").asString();
        String realm = context.getState("realm").asString();
        
        AuthorizationCode authorizationCode = issueCode(scopes, clientId, redirectUri, realm, userState);

        context.putSuccessAddition("code", authorizationCode.get("id").asList().get(0));

        return StageResponse.newBuilder().build();
    }

    private AuthorizationCode issueCode(Set<String> scopes, String clientId, String redirectUri,
                                        String realm, JsonValue user)
            throws ResourceException {

        AuthorizationCode authorizationCode;
        try {
            authorizationCode = issueTokens(
                    clientId,
                    user.get("username").asString(),
                    scopes,
                    redirectUri,
                    realm);
        } catch (InvalidClientException |
                UnsupportedResponseTypeException |
                ServerException | InvalidScopeException |
                CoreTokenException | NotFoundException e) {
            throw ResourceException.newResourceException(400, "Fail to issue code: " + e.getMessage());
        }

        return authorizationCode;
    }

    public AuthorizationCode issueTokens(String clientId, String resourceOwnerId,
                                         Set<String> authorizationScope, String redirectUri, String realm)
            throws InvalidClientException, UnsupportedResponseTypeException, ServerException, InvalidScopeException,
            NotFoundException, CoreTokenException, InvalidGrantException {

        final OAuth2ProviderSettings providerSettings = providerSettingsFactory.get(realm);
        long expiryTime = providerSettings.getAuthorizationCodeLifetime() * 1000 + System.currentTimeMillis();

        final String code = UUID.randomUUID().toString();
        final OpenAMAuthorizationCode authorizationCode
                = OpenAMAuthorizationCode.createOpenAMAuthorizationCode(
                code, resourceOwnerId, clientId,
                redirectUri, authorizationScope, null,
                expiryTime, null, realm,
                "DataStore", null,
                null,
                null, null);


        tokenStore.create(authorizationCode);

        return authorizationCode;
    }
}
