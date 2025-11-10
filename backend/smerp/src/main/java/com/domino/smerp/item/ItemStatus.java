package com.domino.smerp.item;

import com.domino.smerp.item.constants.ItemStatusStatus;
import com.domino.smerp.log.audit.AuditLogEntityListener;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EntityListeners;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.hibernate.envers.Audited;

@Entity
@Getter
@ToString
@Audited
@Builder
@EntityListeners(AuditLogEntityListener.class)
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Table(name = "item_status")
public class ItemStatus {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "item_status_id")
    private Long itemStatusId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private ItemStatusStatus status;
}
