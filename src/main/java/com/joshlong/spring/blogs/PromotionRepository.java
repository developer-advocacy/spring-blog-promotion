package com.joshlong.spring.blogs;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.support.TransactionTemplate;

@Slf4j
@Repository
@RequiredArgsConstructor
class PromotionRepository {

	private final JdbcTemplate ds;

	private final TransactionTemplate tx;

	@EventListener
	public void blogPost(BlogPost post) {
		log.debug("going to persist " + post);
		var sql = """

				""";
		this.ds.execute(sql);

	}

	@EventListener
	public void teammate(TeamEvent event) {
		var teammates = event.teammates();
		log.debug("going to persist " + teammates);
		tx.execute(status -> {
			ds.execute("update spring_teammates set fresh = false");
			log.debug("set all fresh = false");
			teammates.forEach(teammate -> {
				log.debug("persisting " + teammate);
				var sql = """

						insert into spring_teammates (
						    url,
						    name ,
						    position,
						    location,
						    github,
						    twitter ,
						    fresh
						)
						values ( ?, ?, ?, ?, ?, ?, ? )
						on conflict  ( name) do update set
						    url = excluded.url,
						    position = excluded.position,
						    location = excluded.location,
						    github = excluded.github,
						    twitter = excluded.twitter ,
						    fresh = excluded.fresh
						""";
				ds.update(sql, ps -> {
					ps.setString(1, teammate.page().toString());
					ps.setString(2, teammate.name());
					ps.setString(3, teammate.position());
					ps.setString(4, teammate.location());
					ps.setString(5, teammate.twitter());
					ps.setString(6, teammate.github());
					ps.setBoolean(7, true);
					ps.execute();
				});
			});

			return null;
		});

	}

}
