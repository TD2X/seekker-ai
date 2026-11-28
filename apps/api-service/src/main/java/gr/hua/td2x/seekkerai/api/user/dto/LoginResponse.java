
package gr.hua.td2x.seekkerai.api.user.dto;

public record LoginResponse(
        String accessToken,
        String tokenType,
        long expiresIn
) {
}
