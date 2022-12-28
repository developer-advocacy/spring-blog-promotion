package com.joshlong.spring.blogs;

import com.joshlong.spring.blogs.feed.BlogPost;
import com.joshlong.spring.blogs.spring.team.Social;
import com.joshlong.spring.blogs.spring.team.Teammate;
import com.joshlong.spring.blogs.utils.UrlUtils;
import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.sql.Array;
import java.sql.ResultSet;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
class PromotionService {

	private final JdbcTemplate ds;

	private final TransactionTemplate tx;

	public void addBlogPost(BlogPost post) {
		var sql = """
						insert into spring_blog_posts(
						    url,
						    published,
						    title,
						    categories ,
						    author
						)
						values ( ?, ?, ?, ?, ? )
						on conflict (url) do update set
						    published=excluded.published ,
						    title=excluded.title,
						    categories=excluded.categories ,
						    author=excluded.author
				""";
		tx.execute(tx -> {
			ds.update(sql, ps -> {
				ps.setString(1, post.url().toString());
				ps.setDate(2, new java.sql.Date(post.published().toEpochMilli()));
				ps.setString(3, post.title());
				ps.setArray(4, ps.getConnection().createArrayOf("text", post.categories().toArray(new String[0])));
				ps.setString(5, post.author());
				ps.execute();
			});
			return null;
		});
		log.debug(post.url() + " " + post.title());
	}

	public Collection<PromotableBlog> getPromotableBlogs() {
		var sql = """
				select
				    b.title, b.url as blog_url , b.author, b.published, b.categories  ,
				    t.twitter , t.url as profile_url, t.github  , t.name , t.position ,t.location
				 from spring_teammates t , spring_blog_posts b
				 where b.author = t.name and b.promoted is null
				 order by b.published desc
				""";
		return this.ds.query(sql, (rs, rowNum) -> {
			var post = new BlogPost(rs.getString("title"), UrlUtils.buildUrl(rs.getString("blog_url")),
					rs.getString("author"), new Date(rs.getDate("published").getTime()).toInstant(),
					authorsFromArray(rs.getArray("categories")));
			var socialMap = new HashMap<Social, String>();
			Arrays.asList(Social.values()).forEach(s -> addSocialToSocialMediaMap(rs, s, socialMap));
			var author = new Teammate(UrlUtils.buildUrl(rs.getString("profile_url")), rs.getString("name"),
					rs.getString("position"), rs.getString("location"), socialMap);
			return new PromotableBlog(post, author);
		});
	}

	public void promote(BlogPost blog) {
		this.tx.execute(t -> {
			var url = blog.url().toExternalForm();
			log.debug("promoting " + url);
			return this.ds.update("update spring_blog_posts set promoted = NOW() where url = ? ", url);
		});
	}

	@SneakyThrows
	private static Set<String> authorsFromArray(Array array) {
		var stringArray = (String[]) array.getArray();
		return new HashSet<>(Arrays.asList(stringArray));
	}

	@SneakyThrows
	private static void addSocialToSocialMediaMap(ResultSet resultSet, Social social,
			Map<Social, String> socialMediaMap) {
		Optional.ofNullable(resultSet.getString(social.name().toLowerCase()))
				.ifPresent(result -> socialMediaMap.put(social, result));
	}

}
