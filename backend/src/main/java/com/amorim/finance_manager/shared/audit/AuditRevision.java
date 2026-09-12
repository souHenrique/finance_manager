package com.amorim.finance_manager.shared.audit;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.hibernate.envers.RevisionEntity;
import org.hibernate.envers.RevisionNumber;
import org.hibernate.envers.RevisionTimestamp;

@Entity
@Table(name = "audit_revision")
@RevisionEntity
@Getter
@Setter
@NoArgsConstructor
public class AuditRevision {

    @Id
    @GeneratedValue(
            strategy = GenerationType.SEQUENCE,
            generator = "audit_revision_seq"
    )
    @SequenceGenerator(
            name = "audit_revision_seq",
            sequenceName = "audit_revision_seq",
            allocationSize = 1
    )
    @RevisionNumber
    @Column(name = "rev", nullable = false)
    private Integer revision;

    @RevisionTimestamp
    @Column(name = "revtstmp", nullable = false)
    private Long timestamp;
}
