package com.example.libreria.service;

import com.example.libreria.dto.ReservationRequestDTO;
import com.example.libreria.dto.ReservationResponseDTO;
import com.example.libreria.dto.ReturnBookRequestDTO;
import com.example.libreria.model.Book;
import com.example.libreria.model.Reservation;
import com.example.libreria.model.User;
import com.example.libreria.repository.BookRepository;
import com.example.libreria.repository.ReservationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ReservationServiceTest {
    @Mock
    private ReservationRepository reservationRepository;
    @Mock
    private BookRepository bookRepository;
    @Mock
    private BookService bookService;
    @Mock
    private UserService userService;
    @InjectMocks
    private ReservationService reservationService;
    private User testUser;
    private Book testBook;
    private Reservation testReservation;

    @BeforeEach
    void setUp() {
        testUser = new User();
        testUser.setId(1L);
        testUser.setName("Juan Pérez");
        testUser.setEmail("juan@example.com");

        testBook = new Book();
        testBook.setExternalId(258027L);
        testBook.setTitle("The Lord of the Rings");
        testBook.setPrice(new BigDecimal("15.99"));
        testBook.setStockQuantity(10);
        testBook.setAvailableQuantity(5);

        testReservation = new Reservation();
        testReservation.setId(1L);
        testReservation.setUser(testUser);
        testReservation.setBook(testBook);
        testReservation.setRentalDays(7);
        testReservation.setStartDate(LocalDate.now());
        testReservation.setExpectedReturnDate(LocalDate.now().plusDays(7));
        testReservation.setDailyRate(new BigDecimal("15.99"));
        testReservation.setTotalFee(new BigDecimal("111.93"));
        testReservation.setLateFee(BigDecimal.ZERO);
        testReservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        testReservation.setCreatedAt(LocalDateTime.now());
    }

    @Test
    void testCreateReservation_Success() {
        ReservationRequestDTO request = new ReservationRequestDTO(
                testUser.getId(),
                testBook.getExternalId(),
                7,
                LocalDate.now());

        when(userService.getUserEntity(testUser.getId())).thenReturn(testUser);
        when(bookRepository.findByExternalId(testBook.getExternalId())).thenReturn(Optional.of(testBook));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> {
            Reservation reservation = invocation.getArgument(0);
            reservation.setId(1L);
            return reservation;
        });

        ReservationResponseDTO result = reservationService.createReservation(request);

        assertNotNull(result);
        assertEquals(1L, result.getId());
        assertEquals(testUser.getId(), result.getUserId());
        assertEquals(testBook.getExternalId(), result.getBookExternalId());
        assertEquals(new BigDecimal("111.93"), result.getTotalFee());
        assertEquals(BigDecimal.ZERO.setScale(2), result.getLateFee());
        verify(bookService).decreaseAvailableQuantity(testBook.getExternalId());
        verify(reservationRepository).save(any(Reservation.class));
    }

    @Test
    void testCreateReservation_BookNotAvailable() {
        testBook.setAvailableQuantity(0);
        ReservationRequestDTO request = new ReservationRequestDTO(
                testUser.getId(),
                testBook.getExternalId(),
                7,
                LocalDate.now());

        when(userService.getUserEntity(testUser.getId())).thenReturn(testUser);
        when(bookRepository.findByExternalId(testBook.getExternalId())).thenReturn(Optional.of(testBook));

        RuntimeException exception = assertThrows(RuntimeException.class,
                () -> reservationService.createReservation(request));

        assertTrue(exception.getMessage().contains("No hay disponibilidad"));
        verify(bookService, never()).decreaseAvailableQuantity(anyLong());
        verify(reservationRepository, never()).save(any());
    }

    @Test
    void testReturnBook_OnTime() {
        ReturnBookRequestDTO request = new ReturnBookRequestDTO(testReservation.getExpectedReturnDate());

        when(reservationRepository.findById(testReservation.getId())).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponseDTO result = reservationService.returnBook(testReservation.getId(), request);

        assertNotNull(result);
        assertEquals(testReservation.getExpectedReturnDate(), result.getActualReturnDate());
        assertEquals(BigDecimal.ZERO.setScale(2), result.getLateFee());
        assertEquals(Reservation.ReservationStatus.RETURNED, result.getStatus());
        assertEquals(new BigDecimal("111.93"), result.getTotalFee());
        verify(bookService).increaseAvailableQuantity(testBook.getExternalId());
    }

    @Test
    void testReturnBook_Overdue() {
        testReservation.setExpectedReturnDate(LocalDate.now().minusDays(3));
        testReservation.setStartDate(LocalDate.now().minusDays(10));

        ReturnBookRequestDTO request = new ReturnBookRequestDTO(LocalDate.now());

        when(reservationRepository.findById(testReservation.getId())).thenReturn(Optional.of(testReservation));
        when(reservationRepository.save(any(Reservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        ReservationResponseDTO result = reservationService.returnBook(testReservation.getId(), request);

        assertNotNull(result);
        assertEquals(Reservation.ReservationStatus.RETURNED, result.getStatus());
        assertEquals(LocalDate.now(), result.getActualReturnDate());
        assertEquals(new BigDecimal("7.20"), result.getLateFee());
        assertEquals(new BigDecimal("119.13"), result.getTotalFee());
        verify(bookService).increaseAvailableQuantity(testBook.getExternalId());
    }

    @Test
    void testGetReservationById_Success() {
        when(reservationRepository.findById(1L)).thenReturn(Optional.of(testReservation));

        ReservationResponseDTO result = reservationService.getReservationById(1L);

        assertNotNull(result);
        assertEquals(testReservation.getId(), result.getId());
    }

    @Test
    void testGetAllReservations() {
        Reservation reservation2 = new Reservation();
        reservation2.setId(2L);
        reservation2.setUser(testUser);
        reservation2.setBook(testBook);
        reservation2.setRentalDays(5);
        reservation2.setStartDate(LocalDate.now());
        reservation2.setExpectedReturnDate(LocalDate.now().plusDays(5));
        reservation2.setDailyRate(new BigDecimal("10.00"));
        reservation2.setTotalFee(new BigDecimal("50.00"));
        reservation2.setLateFee(BigDecimal.ZERO);
        reservation2.setStatus(Reservation.ReservationStatus.ACTIVE);

        when(reservationRepository.findAll()).thenReturn(Arrays.asList(testReservation, reservation2));

        List<ReservationResponseDTO> result = reservationService.getAllReservations();

        assertNotNull(result);
        assertEquals(2, result.size());
    }

    @Test
    void testGetReservationsByUserId() {
        when(reservationRepository.findByUserId(1L)).thenReturn(Arrays.asList(testReservation));

        List<ReservationResponseDTO> result = reservationService.getReservationsByUserId(1L);

        assertNotNull(result);
        assertEquals(1, result.size());
    }

    @Test
    void testGetActiveReservations() {
        when(reservationRepository.findByStatus(Reservation.ReservationStatus.ACTIVE))
                .thenReturn(Arrays.asList(testReservation));

        List<ReservationResponseDTO> result = reservationService.getActiveReservations();

        assertNotNull(result);
        assertEquals(1, result.size());
    }
}
