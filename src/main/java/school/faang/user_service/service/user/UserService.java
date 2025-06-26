package school.faang.user_service.service.user;

import school.faang.user_service.dto.user.CreateUserDto;
import school.faang.user_service.dto.user.UpdateUserDto;
import school.faang.user_service.dto.user.UserDto;

public interface UserService {

    UserDto create(CreateUserDto userDto);

    UserDto update(long userId, UpdateUserDto userDto);

    UserDto getById(long userId);
}
