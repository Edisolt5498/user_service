package school.faang.user_service.service.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import school.faang.user_service.config.context.UserContext;
import school.faang.user_service.dto.recommendation.CreateRecommendationDto;
import school.faang.user_service.dto.recommendation.RecommendationDto;
import school.faang.user_service.dto.recommendation.RecommendationFilterDto;
import school.faang.user_service.dto.recommendation.UpdateRecommendationDto;
import school.faang.user_service.entity.recommendation.Recommendation;
import school.faang.user_service.entity.user.User;
import school.faang.user_service.exception.DataValidationException;
import school.faang.user_service.exception.ForbiddenException;
import school.faang.user_service.filter.RecommendationAuthorFilter;
import school.faang.user_service.filter.RecommendationContentContainsFilter;
import school.faang.user_service.filter.RecommendationFilter;
import school.faang.user_service.mapper.RecommendationMapper;
import school.faang.user_service.repository.recommendation.RecommendationRepository;
import school.faang.user_service.repository.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertIterableEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Mock
    private RecommendationMapper recommendationMapper;

    @Mock
    private UserContext userContext;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecommendationFilter recommendationFilter;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recommendationService, "minDelayMonths", 6);
    }

    @Test
    void createShouldThrowIfRecommendationToSameUserInSixMonths() {
        long userId = 1L;
        long receiverId = 2L;
        Recommendation prevRecommendation = new Recommendation();
        prevRecommendation.setCreatedAt(LocalDateTime.now().minusDays(1));
        Mockito.when(userContext.getUserId()).thenReturn(userId);
        Mockito.when(
                recommendationRepository.findFirstByAuthorIdAndReceiverIdOrderByCreatedAtDesc(userId, receiverId)
        ).thenReturn(Optional.of(prevRecommendation));
        String content = "Hey Jude!";
        CreateRecommendationDto createDto = new CreateRecommendationDto(
                receiverId,
                content
        );

        assertThrows(DataValidationException.class, () -> recommendationService.create(createDto));
    }

    @Test
    void createShouldThrowIfRecommendationToHimself() {
        long userId = 1L;
        long receiverId = 1L;
        Mockito.when(userContext.getUserId()).thenReturn(userId);
        Mockito.when(
                recommendationRepository.findFirstByAuthorIdAndReceiverIdOrderByCreatedAtDesc(userId, receiverId)
        ).thenReturn(Optional.empty());
        String content = "Hey Jude!";
        CreateRecommendationDto createDto = new CreateRecommendationDto(
                receiverId,
                content
        );

        assertThrows(ForbiddenException.class, () -> recommendationService.create(createDto));
    }

    @Test
    void create() {
        ReflectionTestUtils.setField(recommendationService, "minDelayMonths", 6);
        long userId = 1L;
        long receiverId = 2L;
        Mockito.when(userContext.getUserId()).thenReturn(userId);
        Mockito.when(
                recommendationRepository.findFirstByAuthorIdAndReceiverIdOrderByCreatedAtDesc(userId, receiverId)
        ).thenReturn(Optional.empty());
        Mockito.when(recommendationMapper.toRecommendation(Mockito.any())).thenReturn(new Recommendation());
        Mockito.when(recommendationRepository.save(Mockito.any())).thenReturn(new Recommendation());
        String content = "Hey Jude!";
        CreateRecommendationDto createDto = new CreateRecommendationDto(
                receiverId,
                content
        );

        recommendationService.create(createDto);

        Mockito.verify(recommendationRepository, Mockito.times(1)).save(Mockito.any());
    }

    @Test
    void updateShouldThrowIfNonExistentId() {
        long recommendationId = 1;
        UpdateRecommendationDto updateDto = new UpdateRecommendationDto("New content");
        Mockito.when(recommendationRepository.findById(Mockito.anyLong())).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> recommendationService.update(recommendationId, updateDto));
    }

    @Test
    void updateShouldThrowIfDifferentAuthor() {
        long userId = 1;
        long authorId = 2;
        User author = new User();
        author.setId(authorId);
        long recommendationId = 1;
        Recommendation recommendation = new Recommendation();
        recommendation.setAuthor(author);
        Mockito.when(userContext.getUserId()).thenReturn(userId);
        Mockito.when(
                recommendationRepository.findById(recommendationId)
        ).thenReturn(Optional.of(recommendation));
        UpdateRecommendationDto updateDto = new UpdateRecommendationDto("New content");

        assertThrows(ForbiddenException.class, () -> recommendationService.update(recommendationId, updateDto));
    }

    @Test
    void updateShouldUpdateWithDtoAndSave() {
        long userId = 1;
        long authorId = 1;
        User author = new User();
        author.setId(authorId);
        long recommendationId = 1;
        Recommendation recommendation = new Recommendation();
        recommendation.setAuthor(author);
        Mockito.when(userContext.getUserId()).thenReturn(userId);
        Mockito.when(
                recommendationRepository.findById(recommendationId)
        ).thenReturn(Optional.of(recommendation));
        Mockito.when(
                recommendationRepository.save(recommendation)
        ).thenReturn(recommendation);
        UpdateRecommendationDto updateDto = new UpdateRecommendationDto("New content");

        recommendationService.update(recommendationId, updateDto);

        Mockito.verify(
                recommendationMapper,
                Mockito.times(1)
        ).update(updateDto, recommendation);
        Mockito.verify(
                recommendationRepository,
                Mockito.times(1)
        ).save(recommendation);
    }

    @Test
    void deleteShouldThrowIfNonExistentId() {
        long recommendationId = 1;
        Mockito.when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> recommendationService.delete(recommendationId));
    }

    @Test
    void deleteShouldThrowIfDifferentAuthor() {
        long userId = 1;
        long authorId = 2;
        User author = new User();
        author.setId(authorId);
        long recommendationId = 1;
        Recommendation recommendation = new Recommendation();
        recommendation.setAuthor(author);
        Mockito.when(userContext.getUserId()).thenReturn(userId);
        Mockito.when(
                recommendationRepository.findById(recommendationId)
        ).thenReturn(Optional.of(recommendation));

        assertThrows(ForbiddenException.class, () -> recommendationService.delete(recommendationId));
    }

    @Test
    void delete() {
        long authorId = 1;
        User author = new User();
        author.setId(authorId);
        long recommendationId = 1;
        Recommendation recommendation = new Recommendation();
        recommendation.setAuthor(author);
        recommendation.setId(recommendationId);
        long userId = 1;
        Mockito.when(userContext.getUserId()).thenReturn(userId);
        Mockito.when(
                recommendationRepository.findById(recommendationId)
        ).thenReturn(Optional.of(recommendation));

        recommendationService.delete(recommendationId);

        Mockito.verify(
                recommendationRepository,
                Mockito.times(1)
        ).deleteByIdAndAuthor_id(recommendationId, recommendation.getAuthor().getId());
    }

    @Test
    void getByFiltersShouldReturnAllIfNoFilters() {
        Recommendation r1 = new Recommendation();
        Recommendation r2 = new Recommendation();
        List<Recommendation> allRecommendations = List.of(r1, r2);
        RecommendationFilter filter1 = new RecommendationContentContainsFilter();
        RecommendationFilter filter2 = new RecommendationAuthorFilter();
        List<RecommendationFilter> filters = List.of(filter1, filter2);
        ReflectionTestUtils.setField(recommendationService, "recommendationFilters", filters);
        Mockito.when(
                recommendationRepository.findAll()
        ).thenReturn(allRecommendations);
        RecommendationFilterDto filterDto = new RecommendationFilterDto(null, null, null);
        Mockito.when(recommendationFilter.isApplicable(filterDto)).thenReturn(false);
        Mockito.when(recommendationMapper.toRecommendationDto(Mockito.any())).thenReturn(new RecommendationDto());

        List<RecommendationDto> matchedRecommendations = recommendationService.getByFilters(filterDto);

        assertIterableEquals(matchedRecommendations, allRecommendations);
    }
}