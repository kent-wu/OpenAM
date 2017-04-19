package org.forgerock.openam.selfservice.stage;

import com.mashape.unirest.http.JsonNode;
import com.mashape.unirest.http.Unirest;
import com.mashape.unirest.http.exceptions.UnirestException;
import org.forgerock.json.JsonValue;
import org.forgerock.json.resource.ResourceException;
import org.forgerock.selfservice.core.ProcessContext;
import org.forgerock.selfservice.core.ProgressStage;
import org.forgerock.selfservice.core.StageResponse;
import org.forgerock.selfservice.core.util.RequirementsBuilder;
import org.forgerock.util.Reject;
import org.json.JSONException;

import javax.inject.Inject;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

public class WeiboUserDetailsStage implements ProgressStage<WeiboUserDetailsConfig> {

    @Inject
    public WeiboUserDetailsStage() {
    }

    public JsonValue gatherInitialRequirements(ProcessContext context, WeiboUserDetailsConfig config) throws ResourceException {
        Reject.ifFalse(context.containsState("user"), "Weibo User registration stage expects user in the context");
        return RequirementsBuilder.newEmptyRequirements();
    }

    public StageResponse advance(ProcessContext context, WeiboUserDetailsConfig config) throws ResourceException {
        String weiboAccessToken = context.getInput().get("weiboAccessToken").required().asString();
        weiboAccessToken = weiboAccessToken.replace("\"", "");

        JsonNode userInfo;
        String uid;
        try {
            JsonNode uidNode = Unirest.get("https://api.weibo.com/2/account/get_uid.json")
                    .queryString("access_token", weiboAccessToken)
                    .asJson().getBody();

            uid = uidNode.getObject().getString("uid");

            userInfo = Unirest.get("https://api.weibo.com/2/users/show.json")
                    .queryString("access_token", weiboAccessToken)
                    .queryString("uid", uid)
                    .asJson().getBody();

        } catch (UnirestException | JSONException e) {
            throw ResourceException.newResourceException(400, "Fail to get user from weibo");
        }

        Map<String, String> userMap = new HashMap<>();
        userMap.put("username", "weibo_" + uid);

        try {
            userMap.put("givenName", userInfo.getObject().getString("name"));
            userMap.put("sn", userInfo.getObject().getString("name"));
        } catch (JSONException e) {
            throw ResourceException.newResourceException(400, "Fail to get name from weibo: " + e.getMessage());
        }

        userMap.put("userPassword", "changeit");
        userMap.put("inetUserStatus", "Active");

        initContextState(context, userMap);

        return StageResponse.newBuilder().build();
    }

    private void initContextState(ProcessContext context, Map<String, String> userMap) {
        JsonValue userState = new JsonValue(userMap);
        context.putState("user", userState);
        context.putState("scopes",getScope(context));
        context.putState("redirectUri",context.getInput().get("redirectUri").required());
        context.putState("clientId",context.getInput().get("clientId").required());
        context.putState("realm",context.getInput().get("realm").required());
    }

    private Set<String> getScope(ProcessContext context) {
        JsonValue scopeInput = context.getInput().get("scope").required();
        String[] scopes = scopeInput.asString().split(" ");

        Set<String> scopeSet = new HashSet<>();
        for (String scope : scopes) {
            scopeSet.add(scope);
        }

        return scopeSet;
    }

}
