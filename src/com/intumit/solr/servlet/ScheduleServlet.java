package com.intumit.solr.servlet;

import java.io.IOException;
import java.lang.reflect.Field;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.UUID;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.quartz.CronTrigger;
import org.quartz.JobDetail;
import org.quartz.Scheduler;
import org.quartz.Trigger;
import org.quartz.impl.StdSchedulerFactory;

import com.intumit.quartz.CommonScheduleSettings;
import com.intumit.quartz.IndexJob;
import com.intumit.quartz.IndexScheduleSettings;
import com.intumit.quartz.NonIndexJob;
import com.intumit.quartz.ScheduleSettings;

import flexjson.JSONSerializer;

public class ScheduleServlet extends HttpServlet {
	private static final long serialVersionUID = 1L;
	public static final String DEFAULT_GROUP_NAME = IndexJob.class.getName();
	public static final String NONINDEX_GROUP_NAME = NonIndexJob.class.getName();
	private static final String web_prefix = "schedule";

	@Override
	protected void doGet(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			if (req.getRequestURI().contains(web_prefix + "/search")) {
				search(req, resp);
			}
		} catch (Exception e) {
			e.printStackTrace();
			log("ScheduleServlet", e);
			resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
		}
	}

	private void search(HttpServletRequest req, HttpServletResponse resp)
			throws Exception {

		ArrayList<ScheduleSettings> list = new ArrayList<ScheduleSettings>();
		// 查詢
		Scheduler sched = StdSchedulerFactory.getDefaultScheduler();

		String gn = StringUtils.defaultString(req.getParameter("gn"), DEFAULT_GROUP_NAME);
		String[] jobsInGroup = sched.getJobNames(gn);

		for (int j = 0; j < jobsInGroup.length; j++) {
			JobDetail jobDetail = sched.getJobDetail(jobsInGroup[j], gn);
			Trigger[] triggers = sched.getTriggersOfJob(jobsInGroup[j], gn);

			for (Trigger trigger: triggers) {
				if (trigger instanceof CronTrigger) {
					list.add(getScheduleSettings(jobDetail, (CronTrigger)trigger));
					System.out.println("- " + jobsInGroup[j]);
					break;
				}
				else {
					System.out.println("Got non-CronTrigger:" + triggers[0] + ", skip it.");
				}
			}
		}
		resp.setContentType("text/plain; charset=UTF-8");
		try {
			Collections.sort(list, new Comparator<ScheduleSettings>() {

				@Override
				public int compare(ScheduleSettings o1, ScheduleSettings o2) {
					return o1.getDescription().compareTo(o2.getDescription());
				}

			});

			String str = new JSONSerializer().exclude("*.class").deepSerialize(list);
			resp.getWriter().write(
					"{total:1,page:1,records:1,rows:" + str + "}");
		}
		catch (Exception e) {
			e.printStackTrace();
		}

	}

	@Override
	protected void doPost(HttpServletRequest req, HttpServletResponse resp)
			throws ServletException, IOException {
		try {
			Scheduler sched = StdSchedulerFactory.getDefaultScheduler();

			if (req.getRequestURI().contains(web_prefix)) {
				String gn = StringUtils.defaultString(req.getParameter("gn"), DEFAULT_GROUP_NAME);

				// 新增
				if ("add".equals(req.getParameter("oper"))) {
					String id = UUID.randomUUID().toString();
					JobDetail jobDetail = new JobDetail(id, gn, getJobClass(req.getParameter("type")));

					jobDetail.setDescription(req.getParameter("description"));
					putProperties(jobDetail, req);

					Trigger trigger = new CronTrigger(id, gn, req.getParameter("expression"));
					trigger.setJobName(id);
					trigger.setJobGroup(gn);
					sched.scheduleJob(jobDetail, trigger);
				} else if ("del".equals(req.getParameter("oper"))) {
					String id = req.getParameter("id");
					boolean succ = sched.deleteJob(id, gn);
					System.out.println("delete " + id + ":" + succ);
					if (!succ) resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
				} else if ("pause".equals(req.getParameter("oper"))) {
					String id = req.getParameter("id");
					sched.pauseTrigger(id, gn);

				} else if ("resume".equals(req.getParameter("oper"))) {
					String id = req.getParameter("id");
					sched.resumeTrigger(id, gn);

				} else if ("trigger".equals(req.getParameter("oper"))) {
					String id = req.getParameter("id");
					sched.triggerJobWithVolatileTrigger(id, gn,
							StdSchedulerFactory.getDefaultScheduler()
									.getTrigger(id, gn).getJobDataMap());

				} else if ("interrupt".equals(req.getParameter("oper"))) {
					String id = req.getParameter("id");
					sched.interrupt(id, gn);

				} else if ("executeStatus".equals(req.getParameter("oper"))) {
					//
				} else {
					// 修改
					String id = req.getParameter("id");

					JobDetail jobDetail = new JobDetail(id, gn, getJobClass(req.getParameter("type")));
					jobDetail.setDescription(req.getParameter("description"));

					putProperties(jobDetail, req);

					Trigger trigger = new CronTrigger(id, gn, req.getParameter("expression"));
					trigger.setJobName(id);
					trigger.setJobGroup(gn);
					trigger.setMisfireInstruction(CronTrigger.MISFIRE_INSTRUCTION_DO_NOTHING);
					sched.rescheduleJob(id, gn, trigger);
					sched.addJob(jobDetail, true);

				}
				resp.setStatus(HttpServletResponse.SC_OK);
			}

		} catch (Exception e) {
			log("ScheduleServlet", e);
			resp.setStatus(HttpServletResponse.SC_NOT_ACCEPTABLE);
		}

	}

	private ScheduleSettings getScheduleSettings(JobDetail jobDetail, CronTrigger cronTrigger) throws ParseException {
		String type = jobDetail.getJobDataMap().getString("type");

		if (IndexJob.class.getName().equals(type)) {
			return new IndexScheduleSettings(jobDetail, cronTrigger);
		}
		else {
			if (NonIndexJob.findRegisteredClass(type) != null)
				return new CommonScheduleSettings(jobDetail, cronTrigger);
		}
		return new IndexScheduleSettings(jobDetail, cronTrigger);
	}

	private Class getJobClass(String type) {
		if (IndexJob.class.getName().equals(type)) {
			return IndexJob.class;
		}
		else {
			Class clazz = NonIndexJob.findRegisteredClass(type);

			if (clazz != null)
				return clazz;
		}
		return IndexJob.class;
	}

	private String[] getDetailProperiesNamesOfJobClass(String type) {
		if (IndexJob.class.getName().equals(type)) {
			return IndexJob.JOB_DETAIL_PROPERTIES;
		}
		else {
			Class clazz = NonIndexJob.findRegisteredClass(type);

			if (clazz != null) {
				try {
					Field f = clazz.getDeclaredField("JOB_DETAIL_PROPERTIES");
					return (String[])f.get(null);
				} catch (Exception e) {
					e.printStackTrace();
					return NonIndexJob.JOB_DETAIL_PROPERTIES;
				}
			}
		}
		return IndexJob.JOB_DETAIL_PROPERTIES;
	}

	private void putProperties(JobDetail jobDetail, HttpServletRequest req) {
		jobDetail.getJobDataMap().clear();
		jobDetail.getJobDataMap().put("port", req.getLocalPort() + "");
		jobDetail.getJobDataMap().put("context_path", req.getContextPath());

		for (String p : ScheduleSettings.TOP_LEVEL_JOB_DETAIL_PROPERTIES) {
			jobDetail.getJobDataMap().put(p, req.getParameter(p));
		}

		for (String p : getDetailProperiesNamesOfJobClass(req.getParameter("type"))) {
			jobDetail.getJobDataMap().put(p, StringUtils.trimToEmpty(req.getParameter("jobDataMap." + p)));
		}
	}
}
