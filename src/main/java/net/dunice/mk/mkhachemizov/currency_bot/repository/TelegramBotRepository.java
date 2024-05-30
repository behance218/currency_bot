package net.dunice.mk.mkhachemizov.currency_bot.repository;

import java.time.LocalDate;

import net.dunice.mk.mkhachemizov.currency_bot.entity.CurrencyRate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;



@Repository
@Transactional
public interface TelegramBotRepository extends JpaRepository<CurrencyRate, String> {

    @Modifying
    @Query("DELETE FROM CurrencyRate c WHERE c.date < :date")
    void deleteOutdatedCurrency(@Param("date") LocalDate date);
}