package school.faang.user_service.service.recommendation;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;
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
import school.faang.user_service.filter.RecommendationFilter;
import school.faang.user_service.mapper.RecommendationMapper;
import school.faang.user_service.mapper.RecommendationMapperImpl;
import school.faang.user_service.repository.recommendation.RecommendationRepository;
import school.faang.user_service.repository.user.UserRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

@ExtendWith(MockitoExtension.class)
class RecommendationServiceImplTest {

    @InjectMocks
    private RecommendationServiceImpl recommendationService;

    @Mock
    private RecommendationRepository recommendationRepository;

    @Spy
    private RecommendationMapper recommendationMapper = new RecommendationMapperImpl();

    @Mock
    private UserContext userContext;

    @Mock
    private UserRepository userRepository;

    @Mock
    private RecommendationFilter filter1;

    @Mock
    private RecommendationFilter filter2;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(recommendationService, "minDelayMonths", 6);
    }

    @Test
    void testCreateThrowsIfRecommendationToSameUserInSixMonths() {
        long userId = 1L;
        long receiverId = 2L;
        Recommendation prevRecommendation = new Recommendation();
        prevRecommendation.setCreatedAt(LocalDateTime.now().minusDays(1));
        Mockito.when(userContext.getUserId()).thenReturn(userId);
        Mockito.when(
                recommendationRepository.findFirstByAuthorIdAndReceiverIdOrderByCreatedAtDesc(userId, receiverId))
                .thenReturn(Optional.of(prevRecommendation));
        String content = "Hey Jude!";
        CreateRecommendationDto createDto = new CreateRecommendationDto(receiverId, content);

        assertThrows(DataValidationException.class, () -> recommendationService.create(createDto));
    }

    @Test
    void testCreateThrowsIfRecommendationToHimself() {
        long userId = 1L;
        long receiverId = 1L;
        Mockito.when(userContext.getUserId()).thenReturn(userId);
        Mockito.when(
                recommendationRepository.findFirstByAuthorIdAndReceiverIdOrderByCreatedAtDesc(userId, receiverId))
                .thenReturn(Optional.empty());
        String content = "Hey Jude!";
        CreateRecommendationDto createDto = new CreateRecommendationDto(receiverId, content);

        assertThrows(ForbiddenException.class, () -> recommendationService.create(createDto));
    }

    @Test
    void testCreateSuccess() {
        ReflectionTestUtils.setField(recommendationService, "minDelayMonths", 6);
        long userId = 1L;
        User author = new User();
        author.setId(userId);
        long receiverId = 2L;
        User receiver = new User();
        receiver.setId(receiverId);
        Mockito.when(userContext.getUserId()).thenReturn(userId);
        Mockito.when(
                recommendationRepository.findFirstByAuthorIdAndReceiverIdOrderByCreatedAtDesc(userId, receiverId))
                .thenReturn(Optional.empty());
        Mockito.when(userRepository.getByIdOrThrow(userId)).thenReturn(author);
        Mockito.when(userRepository.getByIdOrThrow(receiverId)).thenReturn(receiver);
        String content = "Hey Jude!";
        Recommendation resultRecommendation = Recommendation.builder()
                .author(author)
                .receiver(receiver)
                .content(content)
                .build();
        Mockito.when(recommendationRepository.save(Mockito.any())).thenReturn(resultRecommendation);

        CreateRecommendationDto createDto = new CreateRecommendationDto(receiverId, content);

        RecommendationDto returnResult = recommendationService.create(createDto);

        Mockito.verify(recommendationRepository, Mockito.times(1)).save(Mockito.any());
        assertEquals(resultRecommendation.getReceiver().getId(), returnResult.receiverId());
        assertEquals(resultRecommendation.getAuthor().getId(), returnResult.authorId());
        assertEquals(resultRecommendation.getContent(), returnResult.content());
    }

    @Test
    void testUpdateThrowsIfNonExistentId() {
        long recommendationId = 1;
        UpdateRecommendationDto updateDto = new UpdateRecommendationDto("New content");
        Mockito.when(recommendationRepository.findById(Mockito.anyLong())).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> recommendationService.update(recommendationId, updateDto));
    }

    @Test
    void testUpdateThrowsIfDifferentAuthor() {
        long userId = 1;
        long authorId = 2;
        long recommendationId = 1;
        Recommendation recommendation = getRecommendation(recommendationId, authorId, userId, "GO!");

        Mockito.when(userContext.getUserId()).thenReturn(userId);
        Mockito.when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));
        UpdateRecommendationDto updateDto = new UpdateRecommendationDto("New content");

        assertThrows(ForbiddenException.class, () -> recommendationService.update(recommendationId, updateDto));
    }

    @Test
    void testUpdateShouldUpdateWithDtoAndSave() {
        long userId = 1;
        long recommendationId = 1;
        String originalContent = "Old me.";
        String newContent = "Check me!";
        Recommendation recommendation = getRecommendation(recommendationId, userId, 2, originalContent);
        Mockito.when(userContext.getUserId()).thenReturn(userId);
        Mockito.when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));
        Mockito.when(recommendationRepository.save(recommendation)).thenReturn(recommendation);
        UpdateRecommendationDto updateDto = new UpdateRecommendationDto(newContent);

        RecommendationDto resultDto = recommendationService.update(recommendationId, updateDto);

        Mockito.verify(recommendationMapper, Mockito.times(1)).update(updateDto, recommendation);
        Mockito.verify(recommendationRepository, Mockito.times(1)).save(recommendation);
        assertEquals(newContent, resultDto.content());
    }

    @Test
    void testDeleteThrowsIfNonExistentId() {
        long recommendationId = 1;
        Mockito.when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.empty());

        assertThrows(NoSuchElementException.class, () -> recommendationService.delete(recommendationId));
    }

    @Test
    void testDeleteThrowsIfDifferentAuthor() {
        long userId = 1;
        long authorId = 2;
        long recommendationId = 1;
        Recommendation recommendation = getRecommendation(recommendationId, authorId, userId, "Some");
        Mockito.when(userContext.getUserId()).thenReturn(userId);
        Mockito.when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        assertThrows(ForbiddenException.class, () -> recommendationService.delete(recommendationId));
    }

    @Test
    void testDeleteSuccess() {
        long recommendationId = 1;
        long userId = 1;
        Recommendation recommendation = getRecommendation(recommendationId, userId, 2L, "Delete me");
        Mockito.when(userContext.getUserId()).thenReturn(userId);
        Mockito.when(recommendationRepository.findById(recommendationId)).thenReturn(Optional.of(recommendation));

        recommendationService.delete(recommendationId);

        Mockito.verify(recommendationRepository, Mockito.times(1))
                .deleteByIdAndAuthor_id(recommendationId, recommendation.getAuthor().getId());
    }

    @Test
    void testGetByFiltersReturnsAllIfNoFilters() {
        List<Recommendation> allRecommendations = getListForFiltering();
        List<RecommendationFilter> filters = List.of(filter1, filter2);
        ReflectionTestUtils.setField(recommendationService, "recommendationFilters", filters);
        Mockito.when(recommendationRepository.findAll()).thenReturn(allRecommendations);
        RecommendationFilterDto filterDto = new RecommendationFilterDto(null, null, null);

        List<RecommendationDto> matchedRecommendations = recommendationService.getByFilters(filterDto);

        assertEquals(matchedRecommendations.size(), allRecommendations.size());
        assertEquals(matchedRecommendations.get(0).content(), allRecommendations.get(0).getContent());
        assertEquals(matchedRecommendations.get(1).content(), allRecommendations.get(1).getContent());
    }

    @Test
    void testGetByFiltersChecksApplicableFiltersAndApplies() {
        List<Recommendation> allRecommendations = getListForFiltering();
        List<RecommendationFilter> filters = List.of(filter1, filter2);
        ReflectionTestUtils.setField(recommendationService, "recommendationFilters", filters);
        Mockito.when(recommendationRepository.findAll()).thenReturn(allRecommendations);
        Mockito.when(filter1.isApplicable(Mockito.any())).thenReturn(true);
        Mockito.when(filter2.isApplicable(Mockito.any())).thenReturn(true);
        Mockito.when(filter1.apply(Mockito.any(), Mockito.any())).thenReturn(allRecommendations.stream());
        Mockito.when(filter2.apply(Mockito.any(), Mockito.any())).thenReturn(allRecommendations.stream());
        RecommendationFilterDto filterDto = new RecommendationFilterDto(null, null, null);

        recommendationService.getByFilters(filterDto);

        Mockito.verify(filter1, Mockito.times(1)).isApplicable(Mockito.any());
        Mockito.verify(filter2, Mockito.times(1)).isApplicable(Mockito.any());
        Mockito.verify(filter1, Mockito.times(1)).apply(Mockito.any(), Mockito.any());
        Mockito.verify(filter2, Mockito.times(1)).apply(Mockito.any(), Mockito.any());
    }

    @Test
    void testGetByFiltersFiltersOutResults() {
        List<Recommendation> allRecommendations = getListForFiltering();
        List<RecommendationFilter> filters = List.of(filter1);
        ReflectionTestUtils.setField(recommendationService, "recommendationFilters", filters);
        Mockito.when(recommendationRepository.findAll()).thenReturn(allRecommendations);
        RecommendationFilterDto filterDto = new RecommendationFilterDto(null, null, null);
        Mockito.when(filter1.isApplicable(Mockito.any())).thenReturn(true);
        Mockito.when(
                filter1.apply(Mockito.any(), Mockito.any()))
                .thenAnswer((Answer<Stream<Recommendation>>) invocation -> {
                    Stream<Recommendation> recommendationStream = invocation.getArgument(0);
                    return recommendationStream.filter(r -> false);
                });

        List<RecommendationDto> matchedRecommendations = recommendationService.getByFilters(filterDto);

        Mockito.verify(filter1, Mockito.times(1)).apply(Mockito.any(), Mockito.any());
        assertEquals(0, matchedRecommendations.size());
    }

    private Recommendation getRecommendation(long recommendationId, long authorId, long receiverId, String content) {
        User author = new User();
        author.setId(authorId);
        User receiver = new User();
        receiver.setId(receiverId);
        return Recommendation.builder().id(recommendationId).author(author).receiver(receiver).content(content).build();
    }

    private List<Recommendation> getListForFiltering() {
        String content1 = "Hey 1!";
        String content2 = "Hey 2!";
        Recommendation r1 = getRecommendation(1L, 1L, 2L, content1);
        Recommendation r2 = getRecommendation(2L, 2L, 1L, content2);
        return List.of(r1, r2);
    }
}