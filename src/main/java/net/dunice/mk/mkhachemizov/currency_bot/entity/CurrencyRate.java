package net.dunice.mk.mkhachemizov.currency_bot.entity;

import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import lombok.Getter;
import lombok.Setter;

import java.time.LocalDate;

@Entity
@Getter
@Setter
public class CurrencyRate {
    @Id
    private String name;
    private double rate;
    private LocalDate date;
}

