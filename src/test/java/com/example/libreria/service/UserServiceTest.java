package com.example.libreria.service;

import com.example.libreria.dto.UserRequestDTO;
import com.example.libreria.dto.UserResponseDTO;
import com.example.libreria.model.User;
import com.example.libreria.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {
    
    @Mock
    private UserRepository userRepository;
    
    @InjectMocks
    private UserService userService;
    
    private User testUser;
    private UserRequestDTO userRequestDTO;
    
    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Juan Pérez");
        testUser.setEmail("juan@example.com");
        testUser.setPhoneNumber("123456789");
        testUser.setCreatedAt(LocalDateTime.now());
        
        userRequestDTO = new UserRequestDTO();
        userRequestDTO.setName("Juan Pérez");
        userRequestDTO.setEmail("juan@example.com");
        userRequestDTO.setPhoneNumber("123456789");
    }
    
    @Test
    void testCreateUser_Success() {
        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        UserResponseDTO result = userService.createUser(userRequestDTO);
        
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getName(), result.getName());
        assertEquals(testUser.getEmail(), result.getEmail());
        verify(userRepository, times(1)).save(any(User.class));
    }
    
    @Test
    void testCreateUser_EmailAlreadyExists() {
        when(userRepository.existsByEmail(anyString())).thenReturn(true);
        
        assertThrows(RuntimeException.class, () -> {
            userService.createUser(userRequestDTO);
        });
        
        verify(userRepository, never()).save(any(User.class));
    }
    
    @Test
    void testGetUserById_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        
        UserResponseDTO result = userService.getUserById(1L);
        
        assertNotNull(result);
        assertEquals(testUser.getId(), result.getId());
        assertEquals(testUser.getName(), result.getName());
    }
    
    @Test
    void testGetUserById_NotFound() {
        when(userRepository.findById(1L)).thenReturn(Optional.empty());
        
        assertThrows(RuntimeException.class, () -> {
            userService.getUserById(1L);
        });
    }
    
    @Test
    void testGetAllUsers() {
        User user2 = new User();
        user2.setId(2L);
        user2.setName("María García");
        user2.setEmail("maria@example.com");
        
        when(userRepository.findAll()).thenReturn(Arrays.asList(testUser, user2));
        
        List<UserResponseDTO> result = userService.getAllUsers();
        
        assertNotNull(result);
        assertEquals(2, result.size());
    }
    
    @Test
    void testUpdateUser_Success() {
        when(userRepository.findById(1L)).thenReturn(Optional.of(testUser));
        when(userRepository.save(any(User.class))).thenReturn(testUser);
        
        UserResponseDTO result = userService.updateUser(1L, userRequestDTO);
        
        assertNotNull(result);
        verify(userRepository, times(1)).save(any(User.class));
    }
    
    @Test
    void testDeleteUser_Success() {
        when(userRepository.existsById(1L)).thenReturn(true);
        
        userService.deleteUser(1L);
        
        verify(userRepository, times(1)).deleteById(1L);
    }
    
    @Test
    void testDeleteUser_NotFound() {
        when(userRepository.existsById(1L)).thenReturn(false);
        
        assertThrows(RuntimeException.class, () -> {
            userService.deleteUser(1L);
        });
        
        verify(userRepository, never()).deleteById(anyLong());
    }
}
