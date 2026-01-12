package com.cathay.identify.entity;

import com.cathay.identify.enums.Role;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

import java.sql.Timestamp;
import java.util.List;

@Entity
@Table(name = "accounts")
@Data // lombok giúp tự generate ra tất cả getter/setter
@NoArgsConstructor // constructor mặc định
@AllArgsConstructor // constructor có đầy đủ các field
@Builder
public class AccountEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    @Column(name = "id")
    private String id;

    @Column(name = "email", unique = true, nullable = false)
    private String email;

    @Column(name = "hash_password", nullable = true)
    private String hash_password;

    @Column(name = "phone")
    private String phone;

    @Column(name = "name")
    private String name;

    @Column(name = "role")
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private Role role = Role.USER;

    @Column(name = "isActive")
    @Builder.Default
    private boolean isActive = true;

    @Column(name = "avtUrl")
    private String avtUrl;

    @Column(name = "googleId")
    private String googleId;

    @Column(name = "createdAt")
    @CreationTimestamp
    private Timestamp createdAt;

    @Column(name = "updatedAt")
    @UpdateTimestamp
    private Timestamp updatedAt;

    @OneToMany(mappedBy = "account", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<RefreshTokenEntity> rfTokens;
}
