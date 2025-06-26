package school.faang.user_service.controller.user;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import school.faang.user_service.dto.user.CreateUserDto;
import school.faang.user_service.dto.user.UpdateUserDto;
import school.faang.user_service.dto.user.UserDto;
import school.faang.user_service.exception.DataValidationException;
import school.faang.user_service.service.user.UserService;

@Component
@RequiredArgsConstructor
public class UserController {
    private final UserService userService;

    public UserDto create(CreateUserDto userDto) {
        if (userDto.username() == null || userDto.username().isBlank()) {
            throw new DataValidationException("Username should be present!");
        }
        if (userDto.email() == null || userDto.email().isBlank()) {
            throw new DataValidationException("Email should be present!");
        }
        if (userDto.password() == null || userDto.password().isBlank()) {
            throw new DataValidationException("Password should be present!");
        }
        if (userDto.countryId() == null) {
            throw new DataValidationException("Country should be present!");
        }
        return userService.create(userDto);
    }

    public UserDto update(long userId, UpdateUserDto userDto) {
        if (userDto.username() == null || userDto.username().isBlank()) {
            throw new DataValidationException("Username should be present!");
        }
        if (userDto.email() == null || userDto.email().isBlank()) {
            throw new DataValidationException("Email should be present!");
        }
        if (userDto.countryId() == null) {
            throw new DataValidationException("Country should be present!");
        }
        return userService.update(userId, userDto);
    }

    public UserDto getById(long userId) {
        return userService.getById(userId);
    }
}
