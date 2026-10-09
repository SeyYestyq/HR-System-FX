package ru.mirea.hrsystem.service;

import ru.mirea.hrsystem.model.StatisticsDto;
import ru.mirea.hrsystem.repository.VacancyDao;

/**
 * ============================================================================
 * ПРАКТИКА ПО ТЕМЕ: ЧИСТАЯ АРХИТЕКТУРА И БИЗНЕС-ЛОГИКА
 * ИСПОЛНИТЕЛЬ: Дамир (Задача T7)
 * ============================================================================
 * 
 * Тебе нужно реализовать подсчет статистики.
 * 
 * В работе пригодится понимание:
 * - Использование DAO (Data Access Object) для получения данных.
 * - Работа со Stream API в Java для фильтрации и подсчета.
 * 
 * Если нужна теория — она в самом конце файла.
 * 
 * ----------------------------------------------------------------------------
 * КАК ДОЛЖНА РАБОТАТЬ ПРОГРАММА:
 * ----------------------------------------------------------------------------
 * 1. Получить список всех вакансий из `vacancyDao`.
 * 2. Посчитать общее количество, количество активных и в архиве.
 * 3. Посчитать среднюю и максимальную зарплату.
 * 4. Вернуть объект `StatisticsDto`.
 * 
 * ----------------------------------------------------------------------------
 * ШПАРГАЛКА (Код из преподского проекта Student Hub):
 * ----------------------------------------------------------------------------
 * /*
 *     // На основе ru.mirea.studenthub.service.AnalyticsService
 *     public AnalyticsDto calculateStudentStats() {
 *         List<Student> students = studentRepository.findAll();
 *         long total = students.size();
 *         long expelled = students.stream().filter(s -> s.getStatus() == StudentStatus.EXPELLED).count();
 *         return new AnalyticsDto(total, expelled);
 *     }
 * *\/
 */
public class StatisticsService {
    private final VacancyDao vacancyDao;

    public StatisticsService(VacancyDao vacancyDao) {
        this.vacancyDao = vacancyDao;
    }

    public StatisticsDto calculateMetrics() {
        // TODO: Реализовать расчет
        throw new UnsupportedOperationException("Метод не реализован (Задача Дамира)");
    }
}
/*
 * ============================================================================
 * ТЕОРИЯ: DAO и Stream API
 * ============================================================================
 * 1. DAO (Data Access Object) — это слой приложения, который отвечает исключительно
 * за чтение и запись данных. DAO не решает бизнес-задачи, он просто дает сервису данные.
 * Сервис (Service) обращается к интерфейсу DAO, чтобы не зависеть от деталей БД (JDBC/SQL).
 * 
 * 2. Stream API — это конвейер преобразований данных в Java. Основные операции:
 * - filter(условие) — оставляет только подходящие элементы.
 * - map(преобразование) — превращает один объект в другой (например, Vacancy -> BigDecimal).
 * - count() — считает количество элементов после фильтрации.
 * - max() / min() — ищет максимальное/минимальное значение.
 */
