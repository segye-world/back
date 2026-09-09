package com.segye.config;

import com.segye.account.AccountRecord;
import com.segye.account.AccountRecordRepository;
import com.segye.category.Category;
import com.segye.category.CategoryRepository;
import com.segye.category.CategoryType;
import com.segye.member.Member;
import com.segye.member.MemberRepository;
import com.segye.paymentmethod.PaymentMethod;
import com.segye.paymentmethod.PaymentMethodRepository;
import com.segye.schedule.Schedule;
import com.segye.schedule.ScheduleRepository;
import com.segye.todo.Todo;
import com.segye.todo.TodoRepository;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

// 목 데이터이므로 운영 DB에는 넣지 않습니다.
@Profile("!prod")
@Component
public class DataInitializer implements ApplicationRunner {

    private final MemberRepository memberRepository;
    private final CategoryRepository categoryRepository;
    private final ScheduleRepository scheduleRepository;
    private final TodoRepository todoRepository;
    private final AccountRecordRepository accountRecordRepository;
    private final PaymentMethodRepository paymentMethodRepository;
    private final PasswordEncoder passwordEncoder;

    public DataInitializer(MemberRepository memberRepository,
                           CategoryRepository categoryRepository,
                           ScheduleRepository scheduleRepository,
                           TodoRepository todoRepository,
                           AccountRecordRepository accountRecordRepository,
                           PaymentMethodRepository paymentMethodRepository,
                           PasswordEncoder passwordEncoder) {
        this.memberRepository = memberRepository;
        this.categoryRepository = categoryRepository;
        this.scheduleRepository = scheduleRepository;
        this.todoRepository = todoRepository;
        this.accountRecordRepository = accountRecordRepository;
        this.paymentMethodRepository = paymentMethodRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (memberRepository.count() > 0) return;

        String encoded = passwordEncoder.encode("mock-password");
        Member m1 = memberRepository.save(new Member("segye@example.com", encoded));
        Member m2 = memberRepository.save(new Member("hana@example.com", encoded));

        // categories (global — no member FK in entity)
        Category catIncome  = categoryRepository.save(new Category("이달 총 수입", CategoryType.INCOME));
        Category catFood    = categoryRepository.save(new Category("식비",       CategoryType.EXPENSE));
        Category catTransit = categoryRepository.save(new Category("교통비",     CategoryType.EXPENSE));
        Category catShop    = categoryRepository.save(new Category("쇼핑",       CategoryType.EXPENSE));
        Category catCulture = categoryRepository.save(new Category("문화생활",   CategoryType.EXPENSE));
        Category catSide    = categoryRepository.save(new Category("부수입",     CategoryType.INCOME));
        Category catCafe    = categoryRepository.save(new Category("카페",       CategoryType.EXPENSE));
        Category catHanaSal = categoryRepository.save(new Category("하나 월급",  CategoryType.INCOME));
        Category catHanaFood= categoryRepository.save(new Category("하나 식비",  CategoryType.EXPENSE));

        // payment methods (회원별) — 프론트 FinanceSettingsApi 가 보장하는 기본값과 같은 이름
        PaymentMethod m1Salary = paymentMethodRepository.save(new PaymentMethod(m1, "월급"));
        PaymentMethod m1Cash   = paymentMethodRepository.save(new PaymentMethod(m1, "현금"));
        PaymentMethod m1Card   = paymentMethodRepository.save(new PaymentMethod(m1, "신용카드"));
        PaymentMethod m2Salary = paymentMethodRepository.save(new PaymentMethod(m2, "월급"));
        PaymentMethod m2Cash   = paymentMethodRepository.save(new PaymentMethod(m2, "현금"));

        LocalDate today    = LocalDate.now();
        LocalDate tomorrow = today.plusDays(1);

        // schedules
        Schedule morning  = scheduleRepository.save(new Schedule(m1, "아침 운동",      today,    7, 9,  "#F3A3A4"));
        Schedule chicken  = scheduleRepository.save(new Schedule(m1, "클릭까스 태릉점", today,   11, 15, "#9DB7EA"));
        Schedule meeting  = scheduleRepository.save(new Schedule(m1, "내일 회의 준비",  tomorrow,10, 11, "#71B35C"));
        Schedule bookClub = scheduleRepository.save(new Schedule(m2, "하나 독서 모임",  today,   18, 19, "#FFB24D"));

        // todos
        todoRepository.save(new Todo(m1, morning, "백준 알고리즘 실버 2문제", today));

        Todo done = new Todo(m1, morning, "두잉코딩 영어 1일차", today);
        done.update(null, true);
        todoRepository.save(done);

        todoRepository.save(new Todo(m1, meeting, "회의 자료 초안 작성", tomorrow));
        todoRepository.save(new Todo(m2, bookClub, "책 챕터 정리", today));

        // account records
        LocalDateTime t = LocalDateTime.now();
        accountRecordRepository.save(new AccountRecord(m1, catIncome,  m1Salary, 280000L,  t.withHour(9).withMinute(0),   null));
        accountRecordRepository.save(new AccountRecord(m1, catFood,    m1Card,   -12900L,  t.withHour(12).withMinute(20), chicken.getId()));
        accountRecordRepository.save(new AccountRecord(m1, catTransit, m1Card,   -6900L,   t.withHour(8).withMinute(40),  null));
        accountRecordRepository.save(new AccountRecord(m1, catCafe,    m1Cash,   -6900L,   t.minusDays(1).withHour(15).withMinute(10), null));
        accountRecordRepository.save(new AccountRecord(m1, catShop,    m1Card,   -84000L,  t.minusDays(2).withHour(19).withMinute(30), null));
        accountRecordRepository.save(new AccountRecord(m1, catCulture, m1Card,   -42000L,  t.minusDays(3).withHour(20).withMinute(0),  null));
        accountRecordRepository.save(new AccountRecord(m1, catSide,    m1Cash,   120000L,  t.minusDays(5).withHour(18).withMinute(0),  null));
        accountRecordRepository.save(new AccountRecord(m1, catFood,    m1Cash,   -18500L,  t.minusDays(8).withHour(13).withMinute(5),  null));
        accountRecordRepository.save(new AccountRecord(m2, catHanaFood,m2Cash,   -24000L,  t.withHour(18).withMinute(10), bookClub.getId()));
        accountRecordRepository.save(new AccountRecord(m2, catHanaSal, m2Salary, 320000L,  t.withHour(10).withMinute(0),  null));
    }
}
