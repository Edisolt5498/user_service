package school.faang.user_service.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Locale;

@Builder
@Data
public class UserDto {
    private long id;
    private String username;
    private String email;
    private String phone;
    private PreferredContact preference;
    private Locale locale;

    public enum PreferredContact {
        EMAIL, PHONE, TELEGRAM
    }
}

