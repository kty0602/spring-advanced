package org.example.expert.domain.todo.service;

import org.example.expert.client.WeatherClient;
import org.example.expert.domain.common.dto.AuthUser;
import org.example.expert.domain.common.exception.InvalidRequestException;
import org.example.expert.domain.todo.dto.request.TodoSaveRequest;
import org.example.expert.domain.todo.dto.response.TodoResponse;
import org.example.expert.domain.todo.dto.response.TodoSaveResponse;
import org.example.expert.domain.todo.entity.Todo;
import org.example.expert.domain.todo.repository.TodoRepository;
import org.example.expert.domain.user.entity.User;
import org.example.expert.domain.user.enums.UserRole;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
public class TodoServiceTest {
    @Mock
    private TodoRepository todoRepository;
    @Mock
    private WeatherClient weatherClient;
    @InjectMocks
    private TodoService todoService;

    @Test
    @DisplayName("todo 정상 등록")
    public void saveTodo_success() {
        // given
        AuthUser authUser = new AuthUser(3L, "test@example.com", UserRole.USER);
        User user = User.fromAuthUser(authUser);

        TodoSaveRequest request = new TodoSaveRequest("제목1", "내용1");
        String weather = "Sunny";
        Todo todo = new Todo("제목1", "내용1", weather, user);

        given(weatherClient.getTodayWeather()).willReturn(weather);
        given(todoRepository.save(any(Todo.class))).willReturn(todo);

        // when
        TodoSaveResponse response = todoService.saveTodo(authUser, request);

        // then
        assertNotNull(response);
        assertEquals(todo.getTitle(), response.getTitle());
        assertEquals(todo.getContents(), response.getContents());
        assertEquals(todo.getWeather(), response.getWeather());
        assertEquals(user.getId(), response.getUser().getId());
    }

    @Test
    @DisplayName("todo 목록 정상 조회")
    public void listTodo_success() {
        // given
        int page = 1;
        int size = 10;
        Pageable pageable = PageRequest.of(page - 1, size);
        User user = new User("test@example.com", "1234", UserRole.USER);
        Todo todo1 = new Todo("제목1", "내용1", "Sunny", user);
        Todo todo2 = new Todo("제목2", "내용2", "Rainy", user);
        Page<Todo> todos = new PageImpl<>(List.of(todo1, todo2), pageable, 2);

        given(todoRepository.findAllByOrderByModifiedAtDesc(pageable)).willReturn(todos);

        // when
        Page<TodoResponse> response = todoService.getTodos(page, size);

        // then
        assertNotNull(response);
        assertEquals(2, response.getTotalElements());

        assertEquals("제목1", response.getContent().get(0).getTitle());
        assertEquals("내용1", response.getContent().get(0).getContents());

        assertEquals("제목2", response.getContent().get(1).getTitle());
        assertEquals("내용2", response.getContent().get(1).getContents());
    }

    @Test
    @DisplayName("todo를 정상적으로 조회")
    public void getTodo() {
        // given
        AuthUser authUser = new AuthUser(3L, "test@example.com", UserRole.USER);
        User user = User.fromAuthUser(authUser);

        long todoId = 1L;
        Todo todo = new Todo("제목1", "내용1", "Sunny", user);
        ReflectionTestUtils.setField(todo, "id", 1L);

        given(todoRepository.findByIdWithUser(todoId)).willReturn(Optional.of(todo));

        // when
        TodoResponse response = todoService.getTodo(todoId);

        // then
        assertNotNull(response);
        assertEquals(todo.getTitle(), response.getTitle());
        assertEquals(todo.getContents(), response.getContents());
        assertEquals(user.getId(), response.getUser().getId());
        assertEquals(todo.getWeather(), response.getWeather());
    }

    @Test
    @DisplayName("todo가 존재하지 않는 경우")
    public void todo_notFound() {
        // given
        long todoId = 1L;

        given(todoRepository.findByIdWithUser(todoId)).willReturn(Optional.empty());

        // when
        InvalidRequestException exception = assertThrows(InvalidRequestException.class, () -> {
            todoService.getTodo(todoId);
        });

        // then
        assertEquals("Todo not found", exception.getMessage());
    }

}
