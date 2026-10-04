package com.vietduc.plastic.service;

import com.vietduc.plastic.model.Customer;
import com.vietduc.plastic.repository.CustomerRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

/**
 * Service quản lý Khách hàng.
 * - Nếu chưa có KH với SĐT này thì tự tạo mới.
 * - Nếu đã có thì cập nhật tên/địa chỉ nếu thay đổi.
 */
@Service
@RequiredArgsConstructor
public class CustomerService {

    private final CustomerRepository customerRepository;

    /**
     * Lấy hoặc tự tạo khách hàng theo SĐT.
     * Nếu đã có, cập nhật tên + địa chỉ nếu khác.
     */
    public Customer findOrCreate(String name, String phone, String address, String notes) {
        if (phone == null || phone.isBlank()) {
            return null;
        }
        Optional<Customer> existing = customerRepository.findByPhone(phone.trim());
        LocalDateTime now = LocalDateTime.now();
        if (existing.isPresent()) {
            Customer c = existing.get();
            boolean changed = false;
            if (name != null && !name.isBlank() && !name.equals(c.getName())) {
                c.setName(name);
                changed = true;
            }
            if (address != null && !address.isBlank() && !address.equals(c.getAddress())) {
                c.setAddress(address);
                changed = true;
            }
            if (notes != null && !notes.isBlank() && !notes.equals(c.getNotes())) {
                c.setNotes(notes);
                changed = true;
            }
            if (changed) c.setUpdatedAt(now);
            return customerRepository.save(c);
        }
        // Tạo mới
        Customer c = new Customer();
        c.setCustomerCode(Customer.generateCode(customerRepository.count() + 1));
        c.setName(name);
        c.setPhone(phone.trim());
        c.setAddress(address);
        c.setNotes(notes);
        c.setTotalOrders(0);
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        return customerRepository.save(c);
    }

    /**
     * Tăng đếm đơn sau khi tạo đơn thành công.
     */
    public void incrementOrderCount(String phone) {
        if (phone == null) return;
        customerRepository.findByPhone(phone.trim()).ifPresent(c -> {
            c.setTotalOrders((c.getTotalOrders() == null ? 0 : c.getTotalOrders()) + 1);
            c.setUpdatedAt(LocalDateTime.now());
            customerRepository.save(c);
        });
    }

    public List<Customer> getAll() {
        return customerRepository.findAll();
    }

    public Optional<Customer> getById(String id) {
        return customerRepository.findById(id);
    }

    public Optional<Customer> getByPhone(String phone) {
        return customerRepository.findByPhone(phone);
    }

    public List<Customer> search(String keyword) {
        if (keyword == null || keyword.isBlank()) return getAll();
        List<Customer> byName = customerRepository.findByNameContainingIgnoreCase(keyword);
        // cũng tìm theo SĐT nếu keyword là số
        Optional<Customer> byPhone = customerRepository.findByPhone(keyword);
        byPhone.ifPresent(byName::add);
        return byName.stream().distinct().toList();
    }

    public Customer create(Customer c) {
        LocalDateTime now = LocalDateTime.now();
        if (c.getCustomerCode() == null || c.getCustomerCode().isBlank()) {
            c.setCustomerCode(Customer.generateCode(customerRepository.count() + 1));
        }
        c.setCreatedAt(now);
        c.setUpdatedAt(now);
        return customerRepository.save(c);
    }

    public Customer update(String id, Customer c) {
        Customer existing = customerRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy khách hàng: " + id));
        existing.setName(c.getName());
        existing.setPhone(c.getPhone());
        existing.setAddress(c.getAddress());
        existing.setNotes(c.getNotes());
        existing.setUpdatedAt(LocalDateTime.now());
        return customerRepository.save(existing);
    }

    public void delete(String id) {
        customerRepository.deleteById(id);
    }
}
