package com.capstone.ebookstore.controller;

import com.capstone.ebookstore.dto.AddressDto;
import com.capstone.ebookstore.service.AddressService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/v1/addresses")
@RequiredArgsConstructor
public class AddressController {

    private final AddressService addressService;

    @GetMapping
    public ResponseEntity<List<AddressDto.AddressResponse>> list(
            @AuthenticationPrincipal UserDetails userDetails) {
        return ResponseEntity.ok(addressService.getAddresses(userDetails.getUsername()));
    }

    @PostMapping
    public ResponseEntity<AddressDto.AddressResponse> create(
            @AuthenticationPrincipal UserDetails userDetails,
            @Valid @RequestBody AddressDto.AddressRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(addressService.addAddress(userDetails.getUsername(), request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(
            @AuthenticationPrincipal UserDetails userDetails,
            @PathVariable Long id) {
        addressService.deleteAddress(userDetails.getUsername(), id);
        return ResponseEntity.noContent().build();
    }
}
