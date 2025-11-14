package com.example.libreria.service;

import com.example.libreria.dto.ReservationRequestDTO;
import com.example.libreria.dto.ReservationResponseDTO;
import com.example.libreria.dto.ReturnBookRequestDTO;
import com.example.libreria.model.Book;
import com.example.libreria.model.Reservation;
import com.example.libreria.model.User;
import com.example.libreria.repository.BookRepository;
import com.example.libreria.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class ReservationService {
    
    private static final BigDecimal LATE_FEE_PERCENTAGE = new BigDecimal("0.15"); // 15% por día
    
    private final ReservationRepository reservationRepository;
    private final BookRepository bookRepository;
    private final BookService bookService;
    private final UserService userService;
    
    @Transactional
    public ReservationResponseDTO createReservation(ReservationRequestDTO requestDTO) {

        User user = userService.getUserEntity(requestDTO.getUserId());
        Book book = bookRepository.findByExternalId(requestDTO.getBookExternalId())
                .orElseThrow(() -> new RuntimeException(
                        "Libro no encontrado con ID externo: " + requestDTO.getBookExternalId()));
        
        if (book.getAvailableQuantity() == null || book.getAvailableQuantity() <= 0) {
            throw new RuntimeException(
                    "No hay disponibilidad para el libro con ID externo: " + requestDTO.getBookExternalId());
        }
        
        Reservation reservation = new Reservation();
        reservation.setUser(user);
        reservation.setBook(book);
        reservation.setRentalDays(requestDTO.getRentalDays());
        reservation.setStartDate(requestDTO.getStartDate());
        reservation.setExpectedReturnDate(requestDTO.getStartDate().plusDays(requestDTO.getRentalDays()));
        reservation.setDailyRate(book.getPrice());
        reservation.setTotalFee(calculateTotalFee(book.getPrice(), requestDTO.getRentalDays()));
        reservation.setLateFee(BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP));
        reservation.setStatus(Reservation.ReservationStatus.ACTIVE);
        
        bookService.decreaseAvailableQuantity(book.getExternalId());
        Reservation savedReservation = reservationRepository.save(reservation);
        return convertToDTO(savedReservation);
    }
    
    @Transactional
    public ReservationResponseDTO returnBook(Long reservationId, ReturnBookRequestDTO returnRequest) {

        Reservation reservation = reservationRepository.findById(reservationId)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + reservationId));
        
        if (reservation.getStatus() != Reservation.ReservationStatus.ACTIVE) {
            throw new RuntimeException("La reserva ya fue devuelta");
        }
        
        LocalDate returnDate = returnRequest.getReturnDate();
        reservation.setActualReturnDate(returnDate);
        
        long daysLate = 0;
        if (returnDate.isAfter(reservation.getExpectedReturnDate())) {
            daysLate = returnDate.toEpochDay() - reservation.getExpectedReturnDate().toEpochDay();
        }
        
        BigDecimal lateFee = calculateLateFee(reservation.getBook().getPrice(), daysLate);
        reservation.setLateFee(lateFee);
        BigDecimal totalFee = calculateTotalFee(reservation.getDailyRate(), reservation.getRentalDays())
                .add(lateFee);
        reservation.setTotalFee(totalFee);
        reservation.setStatus(Reservation.ReservationStatus.RETURNED);
        
        Reservation savedReservation = reservationRepository.save(reservation);
        bookService.increaseAvailableQuantity(reservation.getBook().getExternalId());
        return convertToDTO(savedReservation);
    }
    
    @Transactional(readOnly = true)
    public ReservationResponseDTO getReservationById(Long id) {
        Reservation reservation = reservationRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Reserva no encontrada con ID: " + id));
        return convertToDTO(reservation);
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getAllReservations() {
        return reservationRepository.findAll().stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getReservationsByUserId(Long userId) {
        return reservationRepository.findByUserId(userId).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getActiveReservations() {
        return reservationRepository.findByStatus(Reservation.ReservationStatus.ACTIVE).stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    @Transactional(readOnly = true)
    public List<ReservationResponseDTO> getOverdueReservations() {
        return reservationRepository.findOverdueReservations(
                        Reservation.ReservationStatus.ACTIVE,
                        LocalDate.now())
                .stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }
    
    private BigDecimal calculateTotalFee(BigDecimal dailyRate, Integer rentalDays) {
        if (dailyRate == null || rentalDays == null) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return dailyRate.multiply(new BigDecimal(rentalDays))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    private BigDecimal calculateLateFee(BigDecimal bookPrice, long daysLate) {
        // 15% del precio del libro por cada día de demora
        if (bookPrice == null || daysLate <= 0) {
            return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
        }
        return bookPrice.multiply(LATE_FEE_PERCENTAGE)
                .multiply(new BigDecimal(daysLate))
                .setScale(2, RoundingMode.HALF_UP);
    }
    
    private ReservationResponseDTO convertToDTO(Reservation reservation) {
        User user = reservation.getUser();
        Book book = reservation.getBook();
        
        ReservationResponseDTO dto = new ReservationResponseDTO();
        dto.setId(reservation.getId());
        dto.setUserId(user != null ? user.getId() : null);
        dto.setUserName(user != null ? user.getName() : null);
        dto.setBookExternalId(book != null ? book.getExternalId() : null);
        dto.setBookTitle(book != null ? book.getTitle() : null);
        dto.setRentalDays(reservation.getRentalDays());
        dto.setStartDate(reservation.getStartDate());
        dto.setExpectedReturnDate(reservation.getExpectedReturnDate());
        dto.setActualReturnDate(reservation.getActualReturnDate());
        dto.setDailyRate(reservation.getDailyRate());
        dto.setTotalFee(reservation.getTotalFee());
        dto.setLateFee(reservation.getLateFee());
        dto.setStatus(reservation.getStatus());
        dto.setCreatedAt(reservation.getCreatedAt());
        return dto;
    }
}
