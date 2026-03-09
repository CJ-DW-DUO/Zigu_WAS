package com.zigu.ziguwas.domains.report.entity;

import com.zigu.ziguwas.domains.item.entity.Item;
import com.zigu.ziguwas.domains.user.entity.User;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Entity
@NoArgsConstructor
@AllArgsConstructor
@Getter
public class Report {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "report_id", nullable = false)
    private Long reportId;

    @ManyToOne
    @JoinColumn(name = "item_id" , nullable = false)
    private Item item;

    @ManyToOne
    @JoinColumn(name = "reporter_id", nullable = false)
    private User user; // 신고자 ID

    @ElementCollection(fetch = FetchType.LAZY)
    @CollectionTable(name = "report_category", joinColumns = @JoinColumn(name = "report_id"))
    @Enumerated(EnumType.STRING)
    @Column(name = "rep_category")
    private List<ReportCategory> repCategory; // 신고 카테고리

    @Column
    private String reason;


}
