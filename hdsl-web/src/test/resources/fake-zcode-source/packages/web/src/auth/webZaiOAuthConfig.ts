export const zaiOAuthConfig = {
    // ZAI 当前 OAuth 授权入口使用 /api/oauth 前缀，继续走 /auth/oauth 会打开旧入口。
    authorizeUrl: buildZaiOAuthAuthorizeUrl(""),
    tokenUrl: "/api/v1/oauth/token",
    clientId: "client_public",
};
