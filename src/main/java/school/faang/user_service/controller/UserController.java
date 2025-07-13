package school.faang.user_service.controller;

import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import school.faang.user_service.dto.UserDto;

import java.util.List;
import java.util.Locale;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    @GetMapping("/{id}")
    public UserDto getUser(@PathVariable long id) {
        return UserDto.builder()
                .id(id)
                .email("some-email.com")
                .username("some-username")
                .phone("some-phone")
                .locale(Locale.ENGLISH)
                .preference(UserDto.PreferredContact.EMAIL)
                .build();
    }

    @GetMapping("/{id}/subscribers")
    List<UserDto> getUserSubscribers(@PathVariable long id) {
        long someId = id + 1;
        return List.of(
                UserDto.builder()
                        .id(someId)
                        .email("some-email.com")
                        .username("some-username")
                        .phone("some-phone")
                        .locale(Locale.ENGLISH)
                        .preference(UserDto.PreferredContact.EMAIL)
                        .build(),
                UserDto.builder()
                        .id(someId + 1)
                        .email("some-email.com")
                        .username("some-username")
                        .phone("some-phone")
                        .locale(Locale.ENGLISH)
                        .preference(UserDto.PreferredContact.EMAIL)
                        .build()
        );
    }
}
