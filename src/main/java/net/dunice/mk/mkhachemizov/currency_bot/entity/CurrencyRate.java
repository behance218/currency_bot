package net.dunice.mk.mkhachemizov.currency_bot.entity;

import java.time.LocalDate;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;


@Entity
@Getter
@Setter
public class CurrencyRate {
    @Id
    private String name;
    private double rate;
    private LocalDate date;
}

