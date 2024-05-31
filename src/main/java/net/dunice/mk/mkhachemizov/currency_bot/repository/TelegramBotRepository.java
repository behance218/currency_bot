package net.dunice.mk.mkhachemizov.currency_bot.repository;

import net.dunice.mk.mkhachemizov.currency_bot.entity.CurrencyRate;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;


@Repository
@Transactional
public interface TelegramBotRepository extends JpaRepository<CurrencyRate, String> {
    @Modifying
    @Query("DELETE FROM CurrencyRate")
    void deleteCurrencies();

    @Query("""
                FROM CurrencyRate c
                WHERE c.name ILIKE :name
            """)
    CurrencyRate findByName(String name);
}