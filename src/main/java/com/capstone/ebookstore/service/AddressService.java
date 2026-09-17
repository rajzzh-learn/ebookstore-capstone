package com.capstone.ebookstore.service;

import com.capstone.ebookstore.dto.AddressDto;
import com.capstone.ebookstore.entity.Address;
import com.capstone.ebookstore.entity.User;
import com.capstone.ebookstore.exception.ResourceNotFoundException;
import com.capstone.ebookstore.repository.AddressRepository;
import com.capstone.ebookstore.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class AddressService {

    private final AddressRepository addressRepository;
    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public List<AddressDto.AddressResponse> getAddresses(String email) {
        User user = getUser(email);
        return addressRepository.findByUserId(user.getId())
                .stream().map(this::toResponse).toList();
    }

    @Transactional
    public AddressDto.AddressResponse addAddress(String email, AddressDto.AddressRequest request) {
        User user = getUser(email);
        Address address = Address.builder()
                .user(user)
                .fullName(request.getFullName())
                .street(request.getStreet())
                .city(request.getCity())
                .state(request.getState())
                .postalCode(request.getPostalCode())
                .country(request.getCountry())
                .phoneNumber(request.getPhoneNumber())
                .build();
        return toResponse(addressRepository.save(address));
    }

    @Transactional
    public void deleteAddress(String email, Long id) {
        User user = getUser(email);
        Address address = addressRepository.findByIdAndUserId(id, user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("Address not found: " + id));
        addressRepository.delete(address);
    }

    private User getUser(String email) {
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found"));
    }

    public AddressDto.AddressResponse toResponse(Address a) {
        return AddressDto.AddressResponse.builder()
                .id(a.getId())
                .fullName(a.getFullName())
                .street(a.getStreet())
                .city(a.getCity())
                .state(a.getState())
                .postalCode(a.getPostalCode())
                .country(a.getCountry())
                .phoneNumber(a.getPhoneNumber())
                .build();
    }
}
